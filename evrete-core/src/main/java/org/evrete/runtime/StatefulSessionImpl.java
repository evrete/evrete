package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleMemoryInsertTask;

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
        deltaMemoryManager.clear();
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

        for (RuntimeRuleImpl rule : getRuleStorage()) {
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

}
