package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.memory.SessionMemory;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private boolean active = true;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
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
        fireDefault(new ActivationContext(this));
    }

    private void fireDefault(ActivationContext ctx) {
        processChanges();
        for (RuntimeRuleImpl r : getActiveRules()) {
            r.executeRhs();
        }
        commitMemoryDeltas();
        if (hasMemoryTasks()) {
            fireDefault(ctx);
        }
    }

    private void checkState() {
        if (!active) throw new IllegalStateException("Session has been closed");
    }
}
