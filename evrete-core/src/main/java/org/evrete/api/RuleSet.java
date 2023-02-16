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
     * @param builder rule-builder to create a rule from
     * @return rule type parameter
     * @see #addRule(RuleBuilder)
     */
    default R compileRule(RuleBuilder<?> builder) {
        addRule(builder);
        return getRule(builder.getName());
    }

    /**
     * <p>
     * Compiles the given rule builder into a new {@link Rule} and adds it to the current ruleset.
     * </p>
     *
     * @param builder rule builder
     * @throws RuntimeException instances that can be optionally handled by {@link RuleBuilderExceptionHandler}
     * @see RuleBuilderExceptionHandler
     * @see #setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler)
     */
    void addRule(RuleBuilder<?> builder);

    default boolean ruleExists(String name) {
        return getRule(name) != null;
    }

    R getRule(String name);

    default R getRule(Named named) {
        return getRule(named.getName());
    }

    /**
     * <p>
     *     Sets custom {@link RuleBuilderExceptionHandler} for the ruleset.
     * </p>
     * @param handler exception handler
     * @see RuleBuilderExceptionHandler
     */
    void setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler handler);

    FieldReference[] resolveFieldReferences(String[] args, NamedType.Resolver typeMapper);
}
