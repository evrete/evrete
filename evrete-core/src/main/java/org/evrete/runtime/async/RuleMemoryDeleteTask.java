package org.evrete.runtime.async;

import org.evrete.runtime.RuntimeRule;

import java.util.Collection;

public class RuleMemoryDeleteTask extends Completer {
    private final Collection<RuntimeRule> rules;

    public RuleMemoryDeleteTask(Collection<RuntimeRule> rules) {
        this.rules = rules;
    }

    @Override
    protected void execute() {
        tailCall(
                rules,
                r -> new BetaMemoryDeleteTask(RuleMemoryDeleteTask.this, r)
        );
    }
}
