package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.memory.SessionMemory;

import java.util.Iterator;
import java.util.List;
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
            active = false;
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
        Buffer rawMemoryChanges = getBuffer();

        while (active && rawMemoryChanges.hasTasks()) {
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
    }

    private void fireDefault(ActivationContext ctx) {
        Buffer rawMemoryChanges = getBuffer();

        while (active && rawMemoryChanges.hasTasks()) {
            // Prepare and process memory deltas
            List<RuntimeRule> agenda = processInput(rawMemoryChanges, Action.RETRACT, Action.INSERT);
            rawMemoryChanges.clear();
            activationManager.onAgenda(ctx.incrementFireCount(), agenda);


            Iterator<RuntimeRule> agendaIterator = agenda.iterator();
            while (agendaIterator.hasNext()) {
                RuntimeRuleImpl rule = (RuntimeRuleImpl) agendaIterator.next();
                if (fireCriteria.getAsBoolean() && activationManager.test(rule)) {
                    Buffer buffer = rule.executeRhs();
                    activationManager.onActivation(rule);

                    if (buffer.hasInserts()) {
                        rawMemoryChanges.takeAllFrom(buffer);
                        commitMemoryDeltas();
                        // Reset the state of other rules on the agenda
                        agendaIterator.forEachRemaining(o -> ((RuntimeRuleImpl) o).resetState());
                        // Start over from the beginning
                        break;
                    }

                    if (buffer.hasDeletes()) {
                        rawMemoryChanges.takeAllFrom(buffer);
                        commitMemoryDeltas();

                    }
                }
            }
            commitMemoryDeltas();
        }
    }

}
