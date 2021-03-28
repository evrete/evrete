package org.evrete.api;

import java.util.List;

/**
 * <p>
 * Interface describes a mutable collection of rules. Both {@link Knowledge} and {@link RuleSession}
 * are extending this interface.
 * </p>
 *
 * @param <R> rule type parameter
 */
public interface RuleSet<R extends Rule> {
    /**
     * <p>
     * Method returns a list of rules created so far.
     * </p>
     *
     * @return list of currently known rules
     */
    List<R> getRules();

    /**
     * <p>
     * Builds and saves a new rule from a rule-builder.
     * </p>
     *
     * @param builder rule-builder to create a rule from
     * @return rule type parameter
     */
    R compileRule(RuleBuilder<?> builder);

    default boolean ruleExists(String name) {
        return Named.find(getRules(), name) != null;
    }
}
