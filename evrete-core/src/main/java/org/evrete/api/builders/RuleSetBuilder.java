package org.evrete.api.builders;

import org.evrete.api.FluentEnvironment;
import org.evrete.api.RuntimeContext;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.DSLKnowledgeProvider;

import java.io.IOException;

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
     * Imports all rules using the specified DSL provider and rule source into this ruleset builder.
     *
     * @param provider The DSL provider
     * @param source   Data source containing the rules.
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, Object) for more details
     */
    default RuleSetBuilder<C> importRules(@NonNull DSLKnowledgeProvider provider, Object source) throws IOException {
        provider.appendTo(this, source);
        return this;
    }

    /**
     * Imports rules using the specified DSL provider and rule sources into this ruleset builder.
     *
     * @param dslName The name of the DSL provider.
     * @param source  Data source that contains rule definitions
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @throws IllegalStateException    if no or multiple providers were found for the specified DSL identifier.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, Object) for more details
     */
    default RuleSetBuilder<C> importRules(@NonNull String dslName, Object source) throws IOException {
        return importRules(DSLKnowledgeProvider.load(dslName), source);
    }

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
}
