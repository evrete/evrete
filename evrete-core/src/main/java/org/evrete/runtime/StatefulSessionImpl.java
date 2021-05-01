package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.async.*;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends AbstractRuleSession<StatefulSession> implements StatefulSession {
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.activationManager = newActivationManager();
    }

    private void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        applyActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        applyFireCriteria(fireCriteria);
        return this;
    }

    @Override
    public RuntimeRule getRule(String name) {
        return getRuleStorage().get(name);
    }

    @Override
    public ActivationManager getActivationManager() {
        return activationManager;
    }

    private void applyActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
    }

    @Override
    public void fire() {
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
        purge();
    }

    private void fireContinuous(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (fireCriteria.getAsBoolean() && deltaMemoryManager.hasMemoryChanges()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                    }
                }
            }
            commitRuleDeltas();
            commitBuffer();
        }
    }

    //TODO !!! fix this mess with the buffer and its status, it can be simplified
    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        boolean bufferProcessed = false;
        while (fireCriteria.getAsBoolean() && deltaMemoryManager.hasMemoryChanges()) {
            if (!bufferProcessed) {
                processBuffer();
                bufferProcessed = true;
            }
            agenda = buildMemoryDeltas();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                        // Analyzing buffer
                        int deltaOperations = deltaMemoryManager.deltaOperations();
                        if (deltaOperations > 0) {
                            // Breaking the agenda
                            bufferProcessed = false;
                            break;
                        } else {
                            // Processing deletes if any
                            processBuffer();
                            bufferProcessed = true;
                        }
                    }
                }
                commitRuleDeltas();
            }
            commitBuffer();
        }
    }

    private void processBuffer() {
        MemoryDeltaTask deltaTask = new MemoryDeltaTask(memory.iterator());
        getExecutor().invoke(deltaTask);
        deltaMemoryManager.onDelete(deltaTask.getDeleteMask());
        deltaMemoryManager.onInsert(deltaTask.getInsertMask());
        deltaMemoryManager.clearBufferData();
    }

    private void commitBuffer() {
        memory.commitBuffer();
    }

    private void commitRuleDeltas() {
        for (RuntimeRuleImpl rule : getRuleStorage()) {
            rule.commitDeltas();
        }
    }

    private List<RuntimeRule> buildMemoryDeltas() {
        List<RuntimeRule> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();
        Mask<MemoryAddress> matchMask = deltaMemoryManager.getInsertDeltaMask();

        for (RuntimeRuleImpl rule : getRuleStorage()) {
            boolean ruleAdded = false;

            for (RhsFactGroup group : rule.getLhs().getFactGroups()) {
                if (matchMask.intersects(group.getMemoryMask())) {
                    if (!ruleAdded) {
                        // Mark rule as active
                        affectedRules.add(rule);
                        ruleAdded = true;
                    }

                    if (group instanceof BetaEndNode) {
                        affectedEndNodes.add((BetaEndNode) group);
                    }
                }
            }
        }

        // Ordered task 1 - process beta nodes, i.e. evaluate conditions
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

        deltaMemoryManager.clearDeltaData();
        return affectedRules;
    }

    private void purge() {
        Mask<MemoryAddress> factPurgeMask = deltaMemoryManager.getDeleteDeltaMask();
        if (factPurgeMask.cardinality() > 0) {
            ForkJoinExecutor executor = getExecutor();
            MemoryPurgeTask purgeTask = new MemoryPurgeTask(memory, factPurgeMask);
            executor.invoke(purgeTask);
            Mask<MemoryAddress> emptyKeysMask = purgeTask.getKeyPurgeMask();
            if (emptyKeysMask.cardinality() > 0) {
                // Purging rule beta-memories
                executor.invoke(new ConditionMemoryPurgeTask(getRuleStorage(), emptyKeysMask));
            }
            deltaMemoryManager.clearDeleteData();
        }
    }

}
