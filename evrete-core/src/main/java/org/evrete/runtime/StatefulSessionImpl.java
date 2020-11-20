package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.ActionQueue;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.memory.TypeMemory;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private boolean active = true;
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
        if (active) {
            synchronized (this) {
                if (active) {
                    active = false;
                    super.destroy();
                    knowledge.close(this);
                }
            }
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

        while (active && fireCriteria.getAsBoolean() && ctx.update()) {
            ctx.doDeletions();
            if (!(agenda = ctx.doInserts()).isEmpty()) {
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
                appendToBuffer(memoryActions);
                commitInserts();
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        fireContinuous(ctx);
/*
        ActionQueue<Object> memoryActions = new ActionQueue<>();
        List<RuntimeRule> agenda;

        while (active && fireCriteria.getAsBoolean() && ctx.update()) {
            Collection<TypeMemory> memoriesInsert = ctx.changes.get(Action.INSERT);
            ctx.doDeletions();
            agenda = ctx.doInserts();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                memoryActions.clear();

                boolean skipActivation = false;
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Activate rule and obtain memory changes caused by its execution
                        if (!skipActivation && rule.executeRhs(memoryActions) > 0) {
                            activationManager.onActivation(rule);

                            if(memoryActions.hasActions(Action.INSERT, Action.UPDATE)) {
                                // There are memory changes caused by the rule activation
                                appendToBuffer(memoryActions);
                            }

                            skipActivation = true;

                        }
                    }
                    rule.mergeNodeDeltas();
                }
                // Step 4: Merging memory deltas
                commitDeltas(memoriesInsert);
                // processing rule memory changes (inserts, updates, deletes)

            }
        }
*/
    }

    private void reportStatus(String stage) {
        StringJoiner s = new StringJoiner(", ");
        typeMemories().forEachRemaining(tm -> {
            s.add(tm.reportStatus());
        });
        System.out.println("\t" + stage + ": " + s);
    }

    private void commitInserts() {
        typeMemories().forEachRemaining(TypeMemory::commitDeltas);
    }

    @Override
    //TODO enable support
    public void addConditionTestListener(EvaluationListener listener) {
        throw new UnsupportedOperationException("Currently unsupported for session instances, use knowledge instances instead.");
    }
}
