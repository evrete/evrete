package org.evrete.runtime;

import org.evrete.util.CommonUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class ActivationContext {
    private static final Logger LOGGER = Logger.getLogger(ActivationContext.class.getName());
    private final AtomicInteger activationCount = new AtomicInteger();
    private final SessionMemory memory;
    private final List<SessionRule> rules;
    private final ExecutorService executor;
    private final AbstractRuleSession<?> session;

    public ActivationContext(AbstractRuleSession<?> session, List<SessionRule> rules) {
        this.session = session;
        this.memory = session.getMemory();
        this.executor = session.getService().getExecutor();
        this.rules = Collections.unmodifiableList(rules);
    }

    int incrementFireCount() {
        return this.activationCount.incrementAndGet();
    }

    CompletableFuture<Status> computeDelta(WorkMemoryActionBuffer actions) {
        int bufferedCount = actions.bufferedActionCount();
        LOGGER.fine(()-> "Computing delta memory from [" + bufferedCount + "] actions");
        // 1. Wait for pending actions, if any
        return actions.sinkToSplitView(executor).thenCompose(typedActions -> {

            // Then process the two tasks in sequence:
            // 2.1. Handle delete actions
            // 2.2. Handle insert actions and collect the delta status along the way
            return processDeleteActions(typedActions)
                    .thenCompose(
                            unused -> processDeltaStatus(typedActions)
                    );
        });
    }


    private CompletableFuture<Void> processDeleteActions(Collection<WorkMemoryActionBuffer.SplitView> typedActions) {
        if(typedActions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            //    There are three kinds of memories each delete op must be applied to:
            //       a) alpha-memories
            //       b) fact collections (per type memory) for facts which deletions wasn't yet applied
            //       c) matching fact groups of existing rules

            final MapOfList<TypeAlphaMemory, FactHolder> deletesByAlphaMemory = new MapOfList<>();
            final MapOfList<TypeMemory, DefaultFactHandle> nonAppliedDeletes = new MapOfList<>();
            final MapOfList<SessionFactGroup, FactHolder> deletesByFactGroups = new MapOfList<>();

            for(WorkMemoryActionBuffer.SplitView view : typedActions) {
                ActiveType type = view.getType();
                Collection<DeltaMemoryAction.Delete> deleteOps = view.getDeletes();
                TypeMemory typeMemory = memory.getTypeMemory(type.getId());

                for (DeltaMemoryAction.Delete op : deleteOps) {
                    DefaultFactHandle handle = op.getHandle();
                    FactHolder factHolder = op.getFactWrapper();

                    // a) handling non-applied delete ops
                    if(op.applyToMemory()) {
                        nonAppliedDeletes.add(typeMemory, handle);
                    }

                    // b) splitting by alpha memory
                    type.forEachAlphaAddress(alphaAddress -> {
                        TypeAlphaMemory alphaMemory = memory.getAlphaMemory(alphaAddress);
                        deletesByAlphaMemory.add(alphaMemory, factHolder);
                    });

                    // c) split by fact groups
                    for (SessionRule rule : rules) {
                        for (SessionFactGroup group : rule.getLhs().getFactGroups()) {
                            if (!group.isPlain() && group.getTypeMask().get(type)) {
                                deletesByFactGroups.add(group, factHolder);
                            }
                        }
                    }
                }
            }


            // Turn the collected data into futures
            final Collection<CompletableFuture<Void>> deleteFutures = new ArrayList<>();

            LOGGER.fine(() -> "Scheduling delete ops: alpha memories: [" + deletesByAlphaMemory.size() + "], type memories: [" + nonAppliedDeletes.size() + "],  fact groups: [" + deletesByFactGroups.size() + "]");
            deletesByAlphaMemory.forEach(
                    (memory, value) -> deleteFutures.add(processDeleteDeltaActions(memory, value))
            );

            nonAppliedDeletes.forEach(
                    (memory, factWrappers) -> deleteFutures.add(handleNonAppliedDeletes(memory, factWrappers))
            );

            deletesByFactGroups.forEach(
                    (group, ops) -> deleteFutures.add(group.processDeleteDeltaActions(ops))
            );

            return CommonUtils.completeAll(deleteFutures);
        }
    }

    private CompletableFuture<Status> processDeltaStatus(Collection<WorkMemoryActionBuffer.SplitView> typedActions) {
        if(typedActions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            Status result = new Status();

            // 1. Grouping inserts by affected alpha memories
            MapOfList<AlphaAddress, FactHolder> insertsByAlphaLocation = new MapOfList<>();
            MapOfList<ActiveType.Idx, FactHolder> nonAppliedByTypeMemory = new MapOfList<>();


            for(WorkMemoryActionBuffer.SplitView view : typedActions) {
                ActiveType type = view.getType();
                Collection<DeltaMemoryAction.Insert> insertOps = view.getInserts();
                LOGGER.fine(() -> "Start processing [" + insertOps.size() + "] insert ops of type: " + type.getId());

                // The goal is to compute alpha buckets affected by the insert operations
                // a) Only those rules whose fact types match those alpha buckets should
                //    be selected for activation.
                // b) But before the activation, the rules' beta nodes must be computed.
                //    To do that, we need to find which fact groups of each rule is affected by the
                //    insert operations.

                for (final DeltaMemoryAction.Insert insert : insertOps) {
                    FactHolder factHolder = insert.getFactWrapper();

                    // Splitting insert ops by alpha memories
                    Collection<AlphaAddress> matchingAlphaLocations = session.matchingAlphaLocations(insert.getHandle(), insert.getValues());// type.matchingLocations(session, insert.getValues());
                    for (AlphaAddress matchingAlpha : matchingAlphaLocations) {
                        insertsByAlphaLocation.add(matchingAlpha, factHolder);
                    }

                    // We also need to insert those inserts that were buffered but not actually
                    // saved (those coming from RHS action)
                    if(insert.applyToMemory()) {
                        nonAppliedByTypeMemory.add(insert.getHandle().getType(), factHolder);
                    }
                }
            }
            final List<CompletableFuture<Void>> insertFutures = new LinkedList<>();

            // 2. Preparing insert tasks for each alpha memory
            for (Map.Entry<AlphaAddress, List<FactHolder>> entry : insertsByAlphaLocation.entrySet()) {
                AlphaAddress alpha = entry.getKey();
                List<FactHolder> inserts = entry.getValue();
                TypeAlphaMemory alphaMemory = memory.getAlphaMemory(alpha);
                LOGGER.fine(() -> "Scheduling ["+ inserts.size() +"] inserts into alpha memory: " + alpha);
                // Saving the task...
                insertFutures.add(processInsertDeltaActions(alphaMemory, inserts));
                // Storing the memory for the future commit ops
                result.addAffectedAlphaBucket(alphaMemory);
            }

            // 3. Preparing tasks for non-applied inserts
            for (Map.Entry<ActiveType.Idx, List<FactHolder>> entry : nonAppliedByTypeMemory.entrySet()) {
                ActiveType.Idx type = entry.getKey();
                TypeMemory typeMemory = memory.getTypeMemory(type);
                List<FactHolder> facts = entry.getValue();
                LOGGER.fine(() -> "Scheduling saves into fact storage: " + type + ", fact count: " + facts.size());
                insertFutures.add(this.handleNonAppliedInserts(typeMemory, facts));
            }

            // 4. Identifying which rules (and their condition graphs) are affected by the inserts
            Mask<AlphaAddress> insertMask = Mask.alphaAddressMask().set(insertsByAlphaLocation.keySet());

            for (SessionRule rule : rules) {
                boolean ruleAdded = false;
                for (SessionFactGroup group : rule.getLhs().getFactGroups()) {
                    //if (CommonUtils.intersecting(group.getAlphaAddressMask(), alphaConditionSets)) {
                    if (group.getAlphaAddressMask().intersects(insertMask)) {
                        result.addAffectedFactGroup(group);
                        if (!ruleAdded) {
                            result.addAffectedRule(rule);
                            ruleAdded = true;
                        }
                    }
                }
            }

            // 5. As computing fact groups (Rete graphs) will eventually require data from each graph's
            //    leaf nodes (which are alpha memories of each fact type in the group), we need to process
            //    the alpha tasks first
            return CommonUtils.completeAll(insertFutures)
                    .thenComposeAsync(
                            unused -> {
                                // 5. Now computing the condition fact groups ()
                                return CommonUtils.completeAll(
                                        result.affectedFactGroups,
                                        group -> group.buildDeltas(DeltaMemoryMode.DEFAULT)
                                ).thenApply(unused1 -> result);
                            },
                            executor
                    );
        }
    }


    CompletableFuture<Void> commitMemories(Status status) {
        LOGGER.fine(() -> "Scheduling memory commits. Fact groups: " + status.affectedFactGroups.size() + ", alpha memories: " + status.affectedAlphaBuckets.size());
        final List<CompletableFuture<Void>> commitFutures = new ArrayList<>();
        // 1. Per group tasks (committing beta condition nodes)
        for (SessionFactGroup group : status.affectedFactGroups) {
            commitFutures.add(group.commitDeltas());
        }
        // 2. Committing alpha memories
        for (TypeAlphaMemory alphaMemory : status.affectedAlphaBuckets) {
            commitFutures.add(alphaMemory.commit(executor));
        }
        return CommonUtils.completeAll(commitFutures);
    }

    CompletableFuture<Void> handleNonAppliedInserts(TypeMemory typeMemory, Collection<FactHolder> facts) {
        return CompletableFuture.runAsync(() -> {
                    for (FactHolder fact : facts) {
                        typeMemory.insert(fact);
                    }
                }
                , executor
        );
    }

    CompletableFuture<Void> handleNonAppliedDeletes(TypeMemory typeMemory, Collection<DefaultFactHandle> facts) {
        return CompletableFuture.runAsync(() -> {
                    for (DefaultFactHandle handle : facts) {
                        typeMemory.remove(handle);
                    }
                }
                , executor
        );
    }

    CompletableFuture<Void> processDeleteDeltaActions(TypeAlphaMemory memory, Collection<FactHolder> deletes) {
        return CompletableFuture.runAsync(
                () -> {
                    for (FactHolder delete : deletes) {
                        memory.delete(delete.getFieldValuesId(), delete.getHandle());
                    }
                },
                executor
        );
    }

    CompletableFuture<Void> processInsertDeltaActions(TypeAlphaMemory memory, Collection<FactHolder> inserts) {
        return CompletableFuture.runAsync(
                () -> {
                    for (FactHolder insert : inserts) {
                        memory.insert(insert.getFieldValuesId(), insert.getHandle());
                    }
                },
                executor
        );
    }


    static class Status {
        final List<SessionRule> agenda = new LinkedList<>();
        final List<SessionFactGroup> affectedFactGroups = new LinkedList<>();
        final List<TypeAlphaMemory> affectedAlphaBuckets = new LinkedList<>();

        List<SessionRule> getAgenda() {
            return agenda;
        }

        void addAffectedRule(SessionRule sessionRule) {
            agenda.add(sessionRule);
        }

        void addAffectedFactGroup(SessionFactGroup sessionFactGroup) {
            affectedFactGroups.add(sessionFactGroup);
        }

        void addAffectedAlphaBucket(TypeAlphaMemory typeAlphaMemory) {
            affectedAlphaBuckets.add(typeAlphaMemory);
        }
    }
}
