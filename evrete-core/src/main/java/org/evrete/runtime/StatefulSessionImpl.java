package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.async.RuleMemoryInsertTask;

import java.util.*;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
    private final RuntimeRules ruleStorage;
    private boolean active = true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.activationManager = newActivationManager();
        this.ruleStorage = new RuntimeRules(this);
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor, false);
        }
    }

    private void invalidateSession() {
        this.active = false;
    }

    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }

    private List<RuntimeRule> propagateInsertChanges() {
        List<RuntimeRule> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();
        // Scanning rules first because they are sorted by salience
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.mergeNodeDeltas();
            boolean ruleAdded = false;

            for (TypeMemory tm : this) {
                Type<?> t = tm.getType();
                if (!ruleAdded && rule.dependsOn(t)) {
                    affectedRules.add(rule);
                    ruleAdded = true;
                }

                for (BetaEndNode endNode : rule.getLhs().getAllBetaEndNodes()) {
                    if (endNode.dependsOn(t)) {
                        affectedEndNodes.add(endNode);
                    }
                }
            }
        }

        // Ordered task 1 - process beta nodes, i.e. evaluate conditions
        List<Completer> tasks = new LinkedList<>();
        if (!affectedEndNodes.isEmpty()) {
            tasks.add(new RuleMemoryInsertTask(affectedEndNodes, true));
        }

        if (tasks.size() > 0) {
            ForkJoinExecutor executor = getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }
        return affectedRules;
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        super.setRuleComparator(ruleComparator);
        reSortRules();
    }

    private synchronized RuntimeRuleImpl deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getAllFactTypes()) {
            touchMemory(factType.getFields(), factType.getAlphaMask());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
        return rule;
    }

    @Override
    public RuntimeRule deployRule(RuleDescriptor descriptor) {
        return deployRule(descriptor, true);
    }


    public List<RuntimeRule> getRules() {
        return ruleStorage.asList();
    }

    @Override
    public void clear() {
        super.clear();
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
    }


    @Override
    protected void addListenerToRules(EvaluationListener listener) {
        this.ruleStorage.addListener(listener);
    }

    @Override
    protected void removeListenerFromRules(EvaluationListener listener) {
        this.ruleStorage.removeListener(listener);
    }

    void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    @Override
    public RuntimeRule getRule(String name) {
        return Named.find(getRules(), name);
    }

    @Override
    public void close() {
        synchronized (this) {
            invalidateSession();
            super.destroy();
            knowledge.close(this);
        }
    }

    @Override
    public StatefulSession addImport(String imp) {
        super.addImport(imp);
        return this;
    }

    @Override
    public StatefulSession addImport(Class<?> type) {
        super.addImport(type);
        return this;
    }

    @Override
    public ActivationManager getActivationManager() {
        return activationManager;
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
        return this;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        super.setActivationManagerFactory(managerClass);
        this.activationManager = newActivationManager();
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
        return this;
    }

    @Override
    public void fire() {
        switch (getAgendaMode()) {
            case DEFAULT:
                fireDefault(new ActivationContext(this));
                break;
            case CONTINUOUS:
                fireContinuous(new ActivationContext(this));
                break;
            default:
                throw new IllegalStateException("Unknown mode " + getAgendaMode());
        }
    }

    private void fireContinuous(ActivationContext ctx) {
        List<RuntimeRule> agenda;

        while (hasChanges()) {
            // Mark deleted facts first
            processDeleteBuffer();
            processInsertBuffer();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        if (rule.executeRhs() > 0) {
                            activationManager.onActivation(rule);
                        }
                    }
                }
                commitInserts();
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;

        while (hasChanges()) {
            // Mark deleted facts first
            processDeleteBuffer();
            processInsertBuffer();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Activate rule and obtain memory changes caused by its execution
                        if (rule.executeRhs() > 0) {
                            activationManager.onActivation(rule);
                            if (hasActions(Action.INSERT)) {
                                // Start over
                                break;
                            } else {
                                // Process deletes and continue
                                processDeleteBuffer();
                            }
                        }
                    }
                }
                commitInserts();
            }
        }
    }

    // TODO !!! can be optimized
    private boolean hasChanges() {
        return active && fireCriteria.getAsBoolean() && hasActions(Action.INSERT, Action.RETRACT);
    }

    // TODO !!! can be optimized
    private boolean hasActions(Action... actions) {
        for (TypeMemory tm : this) {
            if (tm.bufferContains(actions)) {
                return true;
            }
        }
        return false;
    }

    private void commitInserts() {
        typeMemories().forEachRemaining(TypeMemory::commitDeltas);
    }
}
