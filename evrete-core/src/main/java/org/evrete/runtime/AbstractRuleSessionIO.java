package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.RuleSession;
import org.evrete.api.SessionLifecycleListener;
import org.evrete.runtime.async.*;
import org.evrete.util.ForkJoinExecutor;

import java.util.*;

abstract class AbstractRuleSessionIO<S extends RuleSession<S>> extends AbstractRuleSession<S> {

    AbstractRuleSessionIO(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    void fireInner() {
        for (SessionLifecycleListener e : lifecycleListeners) {
            e.onEvent(SessionLifecycleListener.Event.PRE_FIRE);
        }
        switch (getAgendaMode()) {
            case DEFAULT:
                fireDefault(new ActivationContext());
                break;
            case CONTINUOUS:
                fireContinuous(new ActivationContext());
                break;
            default:
                throw new IllegalStateException("Unknown mode " + getAgendaMode());
        }
    }

    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRuleImpl> agenda;
        Mask<MemoryAddress> deleteMask = Mask.addressMask();
        FactActionBuffer buff = newActionBuffer();
        while (fireCriteriaMet() && actionBuffer.hasData()) {
            DeltaMemoryStatus deltaStatus = buildDeltaMemory();
            agenda = deltaStatus.getAgenda();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (RuntimeRuleImpl rule : agenda) {
                    if (activationManager.test(rule)) {
                        activationManager.onActivation(rule, rule.callRhs(buff));
                        buff.copyToAndClear(actionBuffer);
                    }
                }
            }
            deltaStatus.commitDeltas();
            deleteMask.or(deltaStatus.getDeleteMask());
        }
        purge(deleteMask);
    }

    private void fireContinuous(ActivationContext ctx) {
        List<RuntimeRuleImpl> agenda;
        Mask<MemoryAddress> deleteMask = Mask.addressMask();
        FactActionBuffer buff = newActionBuffer();
        while (fireCriteriaMet() && actionBuffer.hasData()) {
            DeltaMemoryStatus deltaStatus = buildDeltaMemory();
            agenda = deltaStatus.getAgenda();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (RuntimeRuleImpl rule : agenda) {
                    if (activationManager.test(rule)) {
                        activationManager.onActivation(rule, rule.callRhs(buff));
                    }
                }
                buff.copyToAndClear(actionBuffer);
            }
            deltaStatus.commitDeltas();
            deleteMask.or(deltaStatus.getDeleteMask());
        }
        purge(deleteMask);
    }

    private DeltaMemoryStatus buildDeltaMemory() {
        // Compute entry done deltas
        ComputeDeltaMemoryTask deltaTask = new ComputeDeltaMemoryTask(actionBuffer, memory);


        getExecutor().invoke(deltaTask);
        Mask<MemoryAddress> deleteMask = deltaTask.getDeleteMask();
        Collection<KeyMemoryBucket> bucketsToCommit = deltaTask.getBucketsToCommit();

        Mask<MemoryAddress> insertMask = Mask.addressMask();
        for (KeyMemoryBucket v : bucketsToCommit) {
            insertMask.set(v.address);
        }

        // Compute beta-nodes' deltas
        List<RuntimeRuleImpl> agenda = buildMemoryDeltas(insertMask);

        //DeltaMemoryStatus status = deltaTask.getDeltaMemoryStatus();

        DeltaMemoryStatus status = new DeltaMemoryStatus(deleteMask, bucketsToCommit, agenda);
        //status.setAgenda(agenda);

        actionBuffer.clear();
        return status;
    }

    private List<RuntimeRuleImpl> buildMemoryDeltas(Mask<MemoryAddress> matchMask) {
        List<RuntimeRuleImpl> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();

        for (RuntimeRuleImpl rule : ruleStorage) {
            boolean ruleAdded = false;

            for (RhsFactGroup group : rule.getLhs().getFactGroups()) {
                if (matchMask.intersects(group.getMemoryMask())) {
                    if (!ruleAdded) {
                        // Marking the rule as active
                        affectedRules.add(rule);
                        ruleAdded = true;
                    }

                    if (group instanceof BetaEndNode) {
                        affectedEndNodes.add((BetaEndNode) group);
                    }
                }
            }
        }

        List<Completer> tasks = new LinkedList<>();
        if (!affectedEndNodes.isEmpty()) {
            tasks.add(new RuleMemoryInsertTask(affectedEndNodes, matchMask, true));
        }

        if (!tasks.isEmpty()) {
            ForkJoinExecutor executor = getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }
        return affectedRules;
    }

    private void purge(Mask<MemoryAddress> factPurgeMask) {
        if (factPurgeMask.cardinality() > 0) {
            ForkJoinExecutor executor = getExecutor();
            MemoryPurgeTask purgeTask = new MemoryPurgeTask(memory, factPurgeMask);
            executor.invoke(purgeTask);
            Mask<MemoryAddress> emptyKeysMask = purgeTask.getKeyPurgeMask();
            if (emptyKeysMask.cardinality() > 0) {
                // Purging rule beta-memories
                executor.invoke(new ConditionMemoryPurgeTask(ruleStorage, emptyKeysMask));
            }
        }
    }

    @Override
    void bufferUpdate(FactHandle handle, FactRecord previous, Object updatedFact) {
        bufferUpdate(handle, previous, updatedFact, this.actionBuffer);
    }

    @Override
    void bufferDelete(FactHandle handle) {
        FactRecord existing = getFactRecord(handle);
        if (existing != null) {
            bufferDelete(handle, existing, this.actionBuffer);
        }
    }

    @Override
    public FactHandle insert0(Object fact, boolean resolveCollections) {
        return bufferInsert(fact, resolveCollections, this.actionBuffer);
    }

    @Override
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return bufferInsert(fact, type, resolveCollections, this.actionBuffer);
    }
}
