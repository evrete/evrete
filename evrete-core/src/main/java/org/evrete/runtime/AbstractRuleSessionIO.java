package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.*;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

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
        while (fireCriteriaMet() && actionBuffer.hasData()) {
            DeltaMemoryStatus deltaStatus = buildDeltaMemory(actionBuffer);
            agenda = deltaStatus.getAgenda();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (RuntimeRuleImpl rule : agenda) {
                    if (activationManager.test(rule)) {
                        RuleActivationResult result = rule.executeRhsAndCommitDelta();
                        activationManager.onActivation(rule, result.executions);
                        FactActionBuffer buffer = result.actionBuffer;
                        int ops = buffer.deltaOperations();
                        buffer.copyToAndClear(actionBuffer);
                        if(ops > 0) {
                            // Breaking the agenda
                            break;
                        }
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
        while (fireCriteriaMet() && actionBuffer.hasData()) {
            DeltaMemoryStatus deltaStatus = buildDeltaMemory(actionBuffer);
            agenda = deltaStatus.getAgenda();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (RuntimeRuleImpl rule : agenda) {
                    if (activationManager.test(rule)) {
                        RuleActivationResult result = rule.executeRhsAndCommitDelta();
                        FactActionBuffer buffer = result.actionBuffer;
                        activationManager.onActivation(rule, result.executions);
                        buffer.copyToAndClear(actionBuffer);
                    }
                }
            }
            deltaStatus.commitDeltas();
            deleteMask.or(deltaStatus.getDeleteMask());
        }
        purge(deleteMask);
    }


    private DeltaMemoryStatus buildDeltaMemory(FactActionBuffer buffer) {
        // Compute entry done deltas
        ComputeDeltaMemoryTask deltaTask = new ComputeDeltaMemoryTask(buffer, memory);
        getExecutor().invoke(deltaTask);
        DeltaMemoryStatus status = deltaTask.getDeltaMemoryStatus();
        // Compute beta-nodes' deltas
        List<RuntimeRuleImpl> agenda = buildMemoryDeltas(status.getInsertMask());
        status.setAgenda(agenda);

        buffer.clear();
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

        if (tasks.size() > 0) {
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

    @SuppressWarnings("unchecked")
    <T> T getFactInner(FactHandle handle) {
        AtomicMemoryAction bufferedAction = actionBuffer.find(handle);
        Object found;
        if(bufferedAction == null) {
            found = memory.get(handle.getTypeId()).getFact(handle);
        } else {
            found = bufferedAction.action == Action.RETRACT ? null : bufferedAction.factRecord.instance;
        }
        return (T) found;
    }

    @Override
    void bufferUpdate(FactHandle handle, Object fact) {
        bufferUpdate(handle, fact, this.actionBuffer);
    }

    @Override
    void bufferDelete(FactHandle handle) {
        bufferDelete(handle, this.actionBuffer);
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
