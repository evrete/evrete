package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.async.RuleMemoryInsertTask;

import java.util.*;
import java.util.function.BooleanSupplier;

abstract class AbstractKnowledgeSession<S extends KnowledgeSession<S>> extends AbstractWorkingMemory<S> {
    final RuntimeRules ruleStorage;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
/*
    private final AccessControlContext lhsControlContext;
    private final PrivilegedAction<Void> privilegedProcessBuffer;
*/

    AbstractKnowledgeSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.activationManager = newActivationManager();
        this.ruleStorage = new RuntimeRules();
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor, false);
        }

/*
        this.lhsControlContext = new AccessControlContext(
                new ProtectionDomain[]{
                        getService()
                                .getSecurity()
                                .getProtectionDomain(RuleScope.LHS)
                }
        );

        this.privilegedProcessBuffer = () -> {
            processBufferInsecure();
            return null;
        };
*/
    }

    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }

    private List<RuntimeRule> buildMemoryDeltas() {
        return buildMemoryDeltasInsecure();
    }

    private List<RuntimeRule> buildMemoryDeltasInsecure() {
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

    private synchronized void deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getFactTypes()) {
            memory.touchMemory(factType.getFields(), factType.getAlphaMask());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor, this);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
    }

    @Override
    public void deployRule(RuleDescriptor descriptor) {
        deployRule(descriptor, true);
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
        while (fireCriteria.getAsBoolean() && buffer.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
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
        while (fireCriteria.getAsBoolean() && buffer.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
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
    }

    private void processBuffer() {
        Iterator<AtomicMemoryAction> it = buffer.actions();
        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            int typeId = a.handle.getTypeId();
            TypeMemory tm = getMemory().get(typeId);
            tm.processMemoryChange(a.action, a.handle, a.factRecord);
        }
        buffer.clear();
    }

    private void commitInserts() {
        memory.commitChanges();
    }
}
