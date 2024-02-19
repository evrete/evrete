package org.evrete.runtime;

import org.evrete.api.Rule;

public class RuleBuilderException extends Exception {
    private final Rule rule;

    public RuleBuilderException(Throwable cause, Rule rule) {
        super(cause);
        this.rule = rule;
    }

    public Rule getRule() {
        return rule;
    }
}
