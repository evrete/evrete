package org.evrete.api;

import org.evrete.api.annotations.Nullable;

import java.util.List;

/**
 * <p>
 * Interface describes a mutable collection of rules. Both {@link Knowledge} and {@link RuleSession}
 * are extending this interface.
 * </p>
 *
 * @param <R> rule type parameter
 */
public interface RuleSet<R extends Rule> extends Named {
    /**
     * <p>
     * Method returns a list of rules created so far.
     * </p>
     *
     * @return list of currently known rules
     */
    List<R> getRules();

    /**
     * Checks if a rule with the specified name exists in the rule set.
     *
     * @param name the name of the rule
     * @return {@code true} if a rule with the specified name exists, {@code false} otherwise
     */
    default boolean ruleExists(String name) {
        return getRule(name) != null;
    }

    /**
     * Retrieves a rule based on its name.
     *
     * @param name the name of the rule to retrieve
     * @return the rule with the specified name, or null if no rule with that name exists
     */
    @Nullable
    R getRule(String name);

    /**
     * Retrieves a rule based on the name of the named object.
     *
     * @param named the object with a name
     * @return the rule with the specified name, or null if no rule with that name exists
     */
    default R getRule(Named named) {
        return getRule(named.getName());
    }

}
