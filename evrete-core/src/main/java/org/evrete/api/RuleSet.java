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
     * @param handler exception handler
     **/
    @Deprecated
    default void setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler handler) {
        throw new UnsupportedOperationException("Deprecated");
    }

    FieldReference[] resolveFieldReferences(String[] args, NamedType.Resolver typeMapper);
}
