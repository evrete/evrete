package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleMemoryInsertTask;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

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

    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (fireCriteria.getAsBoolean() && deltaMemoryManager.hasMemoryChanges()) {
            processBuffer();
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
                            break;
                        } else {
                            // Processing deletes if any
                            processBuffer();
                        }
                    }
                }
                commitRuleDeltas();

            }
            commitBuffer();
        }
    }

    private void processBuffer() {
        for (TypeMemory tm : memory) {
            tm.processBuffer();
        }
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
            Mask<MemoryAddress> keyPurgeMask = Mask.addressMask();
            // Purging fact memories
            for (TypeMemory tm : memory) {
                FactStorage<FactRecord> factStorage = tm.factStorage;
                // TODO Predicate could be an instance variable of the TypeMemory class (or a method reference)
                Predicate<FactHandleVersioned> predicate = handle -> {
                    FactRecord fact = factStorage.getFact(handle.getHandle());
                    return fact == null || fact.getVersion() != handle.getVersion();
                };

                for (KeyMemoryBucket bucket : tm.memoryBuckets) {
                    if (factPurgeMask.get(bucket.address)) {
                        if (bucket.purgeDeleted(predicate)) {
                            keyPurgeMask.set(bucket.address);
                        }
                    }
                }
            }

            // Purging rule beta-memories


            deltaMemoryManager.clearDeleteData();
        }
    }

}
