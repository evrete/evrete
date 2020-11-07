package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.ActionQueue;
import org.evrete.runtime.memory.SessionMemory;

import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
        throw new UnsupportedOperationException();
/*
        Buffer rawMemoryChanges = getBuffer();

        while (active && rawMemoryChanges.hasData(Action.values())) {
            // Prepare and process memory deltas
            List<RuntimeRule> agenda = processInput(rawMemoryChanges, Action.RETRACT, Action.INSERT);
            rawMemoryChanges.clear();
            activationManager.onAgenda(ctx.incrementFireCount(), agenda);
            for (RuntimeRule r : agenda) {
                RuntimeRuleImpl rule = (RuntimeRuleImpl) r;

                if (fireCriteria.getAsBoolean() && activationManager.test(rule)) {
                    Buffer buffer = rule.executeRhs();
                    activationManager.onActivation(rule);

                    rawMemoryChanges.takeAllFrom(buffer);
                }
            }
            commitMemoryDeltas();
        }
*/
    }

    private void fireDefault(ActivationContext ctx) {
        Agenda agenda;

        // Do until there are no changes in the memory
        while (active && hasMemoryChanges()) {
            assert !hasAction(Action.UPDATE);

            if (hasAction(Action.RETRACT)) {
                buildMemoryDeltas(Action.RETRACT);
                commitMemoryDeltas();
            }

            if (hasAction(Action.INSERT)) {
                buildMemoryDeltas(Action.INSERT);
                // Get agenda and rules to be activated
                agenda = getAgenda();
                List<RuntimeRule> activeRules = agenda.activeRules();
                // Firing the agenda listener
                activationManager.onAgenda(ctx.incrementFireCount(), activeRules);

                Iterator<RuntimeRule> agendaIterator = activeRules.iterator();

                while (agendaIterator.hasNext()) {

                    RuntimeRuleImpl rule = (RuntimeRuleImpl) agendaIterator.next();
                    if (activationManager.test(rule)) {
                        // The rule fill be fired
                        ActionQueue<Object> ruleBuffer = rule.executeRhs();
                        // Fire another agenda listener
                        activationManager.onActivation(rule);
                        // Merge rule actions into the main memory buffer
                        appendToBuffer(ruleBuffer);

                        // Reset this rule's delta beta-memory
                        rule.resetState();

                        assert !hasAction(Action.UPDATE);

                        if (hasAction(Action.INSERT)) {
                            // The default behaviour is to start over
                            // Committing previous delta changes
                            commitMemoryDeltas();
                            // Committing all rules' delta memories
                            agendaIterator.forEachRemaining(new Consumer<RuntimeRule>() {
                                @Override
                                public void accept(RuntimeRule rule) {
                                    ((RuntimeRuleImpl) rule).resetState();
                                }
                            });

                            // Commit beta memories of the rest of the rules
                            for (RuntimeRule r : agenda.inactiveRules()) {
                                ((RuntimeRuleImpl) r).resetState();
                            }
                            break;
                        }


                    } else {
                        // Merge beta memory deltas event when not fired
                        rule.resetState();
                    }
                }
                // End of agenda
                commitMemoryDeltas();
                // Commit beta memories of the rest of the rules
                for (RuntimeRule r : agenda.inactiveRules()) {
                    ((RuntimeRuleImpl) r).resetState();
                }
            }
        }
    }


}
