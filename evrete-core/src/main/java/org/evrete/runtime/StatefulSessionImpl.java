package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.memory.SessionMemory;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private final long cycleLimit;
    private final boolean ordered;
    private boolean active = true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.cycleLimit = getConfiguration().getCycleLimit();
        this.ordered = getConfiguration().isOrderedExecution();
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
    public void fire() {
        checkState();
        FireContext ctx = new FireContext(this);
        if (ordered) {
            fireOrdered(ctx);
        } else {
            fireUnOrdered(ctx);
        }
    }

    private void fireOrdered(FireContext ctx) {
        doEvaluationTasks(ctx);
        for (RuntimeRule r : getRules()) {
            r.fire();
            if (hasMemoryTasks()) {
                fireOrdered(ctx);
            }
        }
    }

    private void fireUnOrdered(FireContext ctx) {
        doEvaluationTasks(ctx);
        for (RuntimeRule r : getRules()) {
            r.fire();
        }

        if (hasMemoryTasks()) {
            fireUnOrdered(ctx);
        }
    }

    private void doEvaluationTasks(FireContext ctx) {
        int cycle = ctx.incrementFireCount();
        if (cycle > cycleLimit) {
            throw new IllegalStateException("Cycling limit of [" + cycleLimit + "] is reached. You might want to check the rules or increase the limit in configuration.");
        }
        handleBuffer();
    }


    private void checkState() {
        if (!active) throw new IllegalStateException("Session has been closed");
    }
}
