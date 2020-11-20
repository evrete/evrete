package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.ActionQueue;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.memory.TypeMemory;

import java.util.Collection;
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
        // Do until there are no changes in the memory
        RuntimeRules ruleStorage = getRuleStorage();
        ActionQueue<Object> ruleActions = new ActionQueue<>();

        while (active && fireCriteria.getAsBoolean() && ctx.update()) {
            Collection<TypeMemory> memoriesInsert = ctx.changes.get(Action.INSERT);
            Collection<TypeMemory> memoriesDelete = ctx.changes.get(Action.RETRACT);

            if (memoriesDelete.size() > 0) {
                // Clear the memories themselves
                for (TypeMemory tm : memoriesDelete) {
                    tm.performDelete();
                }
            }

            if (memoriesInsert.size() > 0) {
                // Step 1: propagate changes in type memories down to beta-memories
                List<RuntimeRule> agenda = ruleStorage.propagateInsertChanges(memoriesInsert);
                // Step 2: which rules are ready to fire?
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);

                // Step 3: activating rules
                ruleActions.clear();
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Active rule and obtain memory changes caused by its execution
                        int callCount = rule.executeRhs();

                        if (callCount > 0) {
                            activationManager.onActivation(rule);
                            // Append the changes to the session-wide action buffer
                            ruleActions.fillFrom(rule.getRuleBuffer());
                            appendToBuffer(rule.getRuleBuffer());
                        }
                    }
                    rule.mergeNodeDeltas();
                }
                // Step 4: Merging memory deltas
                commitDeltas(memoriesInsert);
                // processing rule memory changes (inserts, updates, deletes)
                appendToBuffer(ruleActions);
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        // Do until there are no changes in the memory
        RuntimeRules ruleStorage = getRuleStorage();
        ActionQueue<Object> ruleActions = new ActionQueue<>();

        while (active && fireCriteria.getAsBoolean() && ctx.update()) {
            Collection<TypeMemory> memoriesInsert = ctx.changes.get(Action.INSERT);
            Collection<TypeMemory> memoriesDelete = ctx.changes.get(Action.RETRACT);

            if (memoriesDelete.size() > 0) {
                // Clear the memories themselves
                for (TypeMemory tm : memoriesDelete) {
                    tm.performDelete();
                }
            }

            if (memoriesInsert.size() > 0) {
                // Step 1: propagate changes in type memories down to beta-memories
                List<RuntimeRule> agenda = ruleStorage.propagateInsertChanges(memoriesInsert);
                // Step 2: which rules are ready to fire?
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);

                // Step 3: activating rules
                ruleActions.clear();
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        // Active rule and obtain memory changes caused by its execution
                        int callCount = rule.executeRhs();

                        if (callCount > 0) {
                            activationManager.onActivation(rule);
                        }
                        // Append the changes to the session-wide action buffer
                        ruleActions.fillFrom(rule.getRuleBuffer());
                        appendToBuffer(rule.getRuleBuffer());
                    }
                    rule.mergeNodeDeltas();
                }
                // Step 4: Merging memory deltas
                commitDeltas(memoriesInsert);
                // processing rule memory changes (inserts, updates, deletes)
                appendToBuffer(ruleActions);
            }
        }
    }

    private void reportStatus(String stage) {
        StringJoiner s = new StringJoiner(", ");
        typeMemories().forEachRemaining(tm -> {
            s.add(tm.reportStatus());
        });
        System.out.println("\t" + stage + ": " + s);
    }

    private void commitDeltas(Collection<TypeMemory> memories) {
        for (TypeMemory tm : memories) {
            tm.commitDeltas();
        }
    }

    @Override
    //TODO enable support
    public void addConditionTestListener(EvaluationListener listener) {
        throw new UnsupportedOperationException("Currently unsupported for session instances, use knowledge instances instead.");
    }
}
