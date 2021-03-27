package org.evrete.api;

import java.util.List;

public interface RuleSet<R extends Rule> {
    List<R> getRules();

    R compileRule(RuleBuilder<?> builder);

    default boolean ruleExists(String name) {
        return Named.find(getRules(), name) != null;
    }
}
