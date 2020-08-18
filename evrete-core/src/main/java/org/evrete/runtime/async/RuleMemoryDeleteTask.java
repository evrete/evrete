package org.evrete.runtime.async;

import org.evrete.runtime.RuntimeRuleImpl;

import java.util.Collection;

public class RuleMemoryDeleteTask extends Completer {
    private final Collection<RuntimeRuleImpl> rules;

    public RuleMemoryDeleteTask(Collection<RuntimeRuleImpl> rules) {
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
