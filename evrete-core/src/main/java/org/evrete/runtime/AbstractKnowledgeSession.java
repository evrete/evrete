package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.async.RuleMemoryInsertTask;
import org.evrete.util.NextIntSupplier;

import java.util.*;
import java.util.function.BooleanSupplier;

abstract class AbstractKnowledgeSession<S extends KnowledgeSession<S>> extends AbstractWorkingMemory<S> {
    private final RuntimeRules ruleStorage;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
    private boolean active = true;

    AbstractKnowledgeSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.activationManager = newActivationManager();
        this.ruleStorage = new RuntimeRules();
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor, false);
        }
    }


    @Override
    protected TypeResolver newTypeResolver() {
        return getParentContext().getTypeResolver().copyOf();
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

        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.mergeNodeDeltas();
            boolean ruleAdded = false;

            for (TypeMemory tm : memory) {
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
            memory.touchMemory(factType.getFields(), factType.getAlphaMask());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor, this);
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

    //@Override
    public void clear() {
        super.clear();
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
    }

    @Override
    public void addListener(EvaluationListener listener) {
        getAlphaConditions().addListener(listener);
        this.ruleStorage.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        getAlphaConditions().removeListener(listener);
        this.ruleStorage.removeListener(listener);
    }

    void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    //@Override
    public RuntimeRule getRule(String name) {
        return Named.find(getRules(), name);
    }

    //@Override
    public void close() {
        synchronized (this) {
            invalidateSession();
            //memory.destroy();
            knowledge.close(this);
        }
    }


    @Override
    public ActivationManager getActivationManager() {
        return activationManager;
    }


    void applyActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        super.setActivationManagerFactory(managerClass);
        this.activationManager = newActivationManager();
    }

    void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
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
        while (active && fireCriteria.getAsBoolean() && buffer.hasData()) {
            processBuffer();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                    }
                }
                commitInserts();
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (active && fireCriteria.getAsBoolean() && buffer.hasData()) {
            processBuffer();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                        // Analyzing buffer
                        int deltaOperations = buffer.deltaOperations();
                        if (deltaOperations > 0) {
                            // Breaking the agenda
                            break;
                        } else {
                            // Processing deletes if any
                            processBuffer();
                        }
                    }
                }
                commitInserts();
            }
        }

/*
        List<RuntimeRule> agenda;

        while (hasChanges()) {
            // Mark deleted facts first
            processDeleteBuffer();
            processInsertBuffer();
            //debug();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Activate rule and obtain memory changes caused by its execution
                        //System.out.println("Executing RHS");
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
            } else {
                System.out.println("No agenda");
            }
        }
*/
    }


    /**
     * @return count of actual memory inserts (both successful INSERT and UPDATE operations)
     */
    private int processBuffer() {
        NextIntSupplier insertCounter = new NextIntSupplier();
        Iterator<AtomicMemoryAction> it = buffer.actions();
        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            int typeId = a.handle.getTypeId();
            TypeMemory tm = getMemory().get(typeId);
            tm.processMemoryChange(a.action, a.handle, a.factRecord, insertCounter);
        }
        buffer.clear();
        return insertCounter.get();
    }

    private void commitInserts() {
        memory.forEachMemory(TypeMemory::commitDeltas);
    }
}
