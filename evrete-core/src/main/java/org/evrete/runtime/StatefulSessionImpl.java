package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.util.ActionQueue;

import java.util.List;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.activationManager = newActivationManager();
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
        ActionQueue<Object> memoryActions = new ActionQueue<>();
        List<RuntimeRule> agenda;

        while (fireCriteria.getAsBoolean() && hasActions(Action.INSERT, Action.RETRACT)) {
            _assertActive();
            // Mark deleted facts first
            doDeletions();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                memoryActions.clear();
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Activate rule and obtain memory changes caused by its execution
                        if (rule.executeRhs(memoryActions) > 0) {
                            activationManager.onActivation(rule);
                        }
                    }
                }
                // processing rule memory changes (inserts, updates, deletes)
                commitInserts();
                appendToBuffer(memoryActions);
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        ActionQueue<Object> memoryActions = new ActionQueue<>();
        List<RuntimeRule> agenda;

        while (fireCriteria.getAsBoolean() && hasActions(Action.INSERT, Action.RETRACT)) {
            _assertActive();
            // Mark deleted facts first
            doDeletions();
            if (!(agenda = propagateInsertChanges()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                memoryActions.clear();
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Activate rule and obtain memory changes caused by its execution
                        if (rule.executeRhs(memoryActions) > 0) {
                            activationManager.onActivation(rule);
                            // Apply rule changes immediately
                            appendToBuffer(memoryActions);
                            memoryActions.clear();
                            // Perform deletes (if any)
                            doDeletions();
                            if (hasActions(Action.INSERT)) {
                                break;
                            }
                        }
                    }
                }
                // processing rule memory changes (inserts, updates, deletes)
                commitInserts();
            }
        }
    }

    private void commitInserts() {
        typeMemories().forEachRemaining(TypeMemory::commitDeltas);
    }
}
