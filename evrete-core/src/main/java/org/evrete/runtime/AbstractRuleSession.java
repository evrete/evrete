package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.*;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractWorkingMemory<S> {
    private final RuntimeRules ruleStorage;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.activationManager = newActivationManager();
        this.ruleStorage = new RuntimeRules();
        // Deploy existing rules
        for (RuleDescriptor descriptor : knowledge.getRules()) {
            deployRule(descriptor, false);
        }
    }

    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }

    @Override
    public RuntimeRule compileRule(RuleBuilder<?> builder) {
        RuleDescriptor rd = compileRuleBuilder(builder);
        return deployRule(rd, true);
    }

    @Override
    public List<RuntimeRule> getRules() {
        return ruleStorage.asList();
    }

    private List<RuntimeRule> buildMemoryDeltas() {
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

                for (BetaEndNode endNode : rule.getLhs().getEndNodes()) {
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

    private synchronized RuntimeRule deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getFactTypes()) {
            Type<?> t = factType.getType();
            TypeMemoryState state = getActiveSate(t);
            TypeMemory tm = memory.getCreateUpdate(state);
            tm.touchMemory(factType.getFields(), factType.getAlphaMask());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor, this);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
        return rule;
    }

    @Override
    public void clear() {
        super.clear();
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
    }

    @Override
    public void addListener(EvaluationListener listener) {
        forEachAlphaCondition(a -> a.addListener(listener));
        this.ruleStorage.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        forEachAlphaCondition(a -> a.removeListener(listener));
        this.ruleStorage.removeListener(listener);
    }

    public void close() {
        synchronized (this) {
            invalidateSession();
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
                fireDefault(new ActivationContext());
                break;
            case CONTINUOUS:
                fireContinuous(new ActivationContext());
                break;
            default:
                throw new IllegalStateException("Unknown mode " + getAgendaMode());
        }
    }

    @Override
    public <T> Future<T> fireAsync(final T result) {
        return getExecutor().submit(this::fire, result);
    }

    private void fireContinuous(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (fireCriteria.getAsBoolean() && actionCounter.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    if (Thread.currentThread().isInterrupted()) return;
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
        while (fireCriteria.getAsBoolean() && actionCounter.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    if (Thread.currentThread().isInterrupted()) return;
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                        // Analyzing buffer
                        int deltaOperations = actionCounter.deltaOperations();
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
    }

    private void processBuffer() {
        //TODO !!!! narrow the scope, all type memories are being processed
        Iterator<TypeMemory> it = memory.iterator();
        getExecutor().invoke(new MemoryDeltaTask(it));
        actionCounter.clear();
    }

    private void commitInserts() {
        memory.commitChanges();
    }
}
