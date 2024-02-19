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
     * @deprecated Since version 3.1.0, for performance reasons, rules are no longer compiled one by one.
     *             As such, the use of this exception handler has become obsolete.
     *             Please join our discussions on GitHub to propose a new approach.
     **/
    @Deprecated
    default void setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler ignored) {
        throw new UnsupportedOperationException("Deprecated");
    }

    FieldReference[] resolveFieldReferences(String[] args, NamedType.Resolver typeMapper);
}
