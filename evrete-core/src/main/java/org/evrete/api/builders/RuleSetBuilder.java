package org.evrete.api.builders;

import org.evrete.api.FluentEnvironment;
import org.evrete.api.RuntimeContext;

/**
 * `RuleSetBuilder` is an interface intended for building and appending rules to the current runtime context.
 *
 * @param <C> Represents the type of the RuntimeContext.
 */
public interface RuleSetBuilder<C extends RuntimeContext<C>> extends FluentEnvironment<RuleSetBuilder<C>> {

    /**
     * Creates a new rule with the provided name.
     *
     * @param name rule name
     * @return a new rule builder
     */
    RuleBuilder<C> newRule(String name);

    /**
     * Creates a new unnamed rule.
     *
     * @return a new rule builder
     */
    RuleBuilder<C> newRule();


    /**
     * <p>
     * Builds and appends previously created rules to the current context.
     * </p>
     *
     * @return the current context after appending new rules
     */
    C build();


    /**
     * <p>
     * Returns the builder's context.
     * </p>
     *
     * @return the current context
     */
    C getContext();

    /**
     * Returns the current {@code ClassLoader} associated with this builder.
     * By default, each builder inherits the class loader from the {@link #getContext()} instance.
     *
     * @return The current {@code ClassLoader}.
     */
    ClassLoader getClassLoader();
}
