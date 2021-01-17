package org.evrete.runtime;

import org.evrete.api.*;

import java.util.List;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
    private boolean active = true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.activationManager = newActivationManager();
    }

    private void invalidateSession() {
        this.active = false;
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

    private boolean hasChanges() {
        return active && fireCriteria.getAsBoolean() && hasActions(Action.INSERT, Action.RETRACT);
    }

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
