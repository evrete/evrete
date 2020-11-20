/*
package org.evrete.runtime.async;

import org.evrete.api.Type;
import org.evrete.runtime.RuntimeRuleImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RuleMemoryDeleteTask extends Completer {
    private final Map<RuntimeRuleImpl, List<Type<?>>> rules;

    public RuleMemoryDeleteTask(Map<RuntimeRuleImpl, List<Type<?>>> rules) {
        this.rules = rules;
    }

    @Override
    protected void execute() {
        tailCall(
                rules.entrySet(),
                e -> new BetaMemoryDeleteTask(RuleMemoryDeleteTask.this, e.getKey(), e.getValue())
        );
    }
}
*/
