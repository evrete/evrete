package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.memory.SessionMemory;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private final long cycleLimit;
    private boolean active = true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.cycleLimit = getConfiguration().getCycleLimit();
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
        fireDefault(new FireContext(this));
    }

/*
    private void fireSequential(FireContext ctx) {
        doEvaluationTasks(ctx);
        for (RuntimeRule r : getActiveRules()) {
            ((RuntimeRuleImpl) r).executeRhs();
            commitMemoryDeltas();
            if (hasMemoryTasks()) {
                fireSequential(ctx);
            }
        }
    }
*/

    private void fireDefault(FireContext ctx) {
        doEvaluationTasks(ctx);
        for (RuntimeRuleImpl r : getActiveRules()) {
            r.executeRhs();
        }
        commitMemoryDeltas();
        if (hasMemoryTasks()) {
            fireDefault(ctx);
        }
    }

    private void doEvaluationTasks(FireContext ctx) {
        int cycle = ctx.incrementFireCount();
        if (cycle > cycleLimit) {
            throw new IllegalStateException("Cycling limit of [" + cycleLimit + "] is reached. You might want to check the rules or increase the limit in configuration.");
        }
        processChanges();
    }


    private void checkState() {
        if (!active) throw new IllegalStateException("Session has been closed");
    }
}
