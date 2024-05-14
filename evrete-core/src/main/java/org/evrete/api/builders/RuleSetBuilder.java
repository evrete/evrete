package org.evrete.api.builders;

import org.evrete.api.FluentEnvironment;
import org.evrete.api.RuntimeContext;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.DSLKnowledgeProvider;

import java.io.IOException;
import java.util.function.Predicate;

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
     * Imports rulesets using the specified DSL provider and rule sources into this ruleset builder.
     * This method iterates through the provided sources, using the DSL provider to append content
     * to the builder instance based on the specified name filter.
     *
     * @param provider   The DSL provider used to interpret and load the rules from the sources.
     * @param nameFilter A predicate used to filter rulesets by name.
     * @param sources    An array of data sources needed for loading the rules. These could be file paths,
     *                   URIs, or any other sources recognized by the DSL provider.
     * @return This RuleSetBuilder instance, allowing for method chaining.
     * @throws IOException              If there is an issue reading from the sources. This can happen if a source
     *                                  is inaccessible or corrupt.
     * @throws IllegalArgumentException If the source format is not recognized by the DSL provider or
     *                                  cannot be processed for any reason. Implementations should throw
     *                                  this exception to indicate issues with processing the input sources.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, java.util.function.Predicate, Object[]) for more
     * details on how rules are appended to the builder.
     */
    @SuppressWarnings("unchecked")
    default <S> RuleSetBuilder<C> importRulesets(@NonNull DSLKnowledgeProvider provider,
                                             @NonNull Predicate<String> nameFilter,
                                             S... sources) throws IOException {
        provider.appendTo(this, nameFilter, sources);
        return this;
    }

    /**
     * Imports all rules using the specified DSL provider and rule sources into this ruleset builder.
     *
     * @param provider The DSL provider
     * @param sources  Data sources that may be needed for loading the rules.
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, java.util.function.Predicate, Object[]) for more details
     */
    @SuppressWarnings("unchecked")
    default <S> RuleSetBuilder<C> importAllRules(@NonNull DSLKnowledgeProvider provider, S... sources) throws IOException {
        provider.appendTo(this, name -> true, sources);
        return this;
    }

    /**
     * Imports rules using the specified DSL provider and rule sources into this ruleset builder.
     *
     * @param dslName    The name of the DSL provider.
     * @param nameFilter A predicate used to filter rulesets by name.
     * @param sources    Data sources that may be needed for loading the rules.
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @throws IllegalStateException    if no or multiple providers were found for the specified DSL identifier.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, java.util.function.Predicate, Object[]) for more details
     */
    @SuppressWarnings("unchecked")
    default <S> RuleSetBuilder<C> importRulesets(@NonNull String dslName, @NonNull Predicate<String> nameFilter, S... sources) throws IOException {
        return importRulesets(DSLKnowledgeProvider.load(dslName), nameFilter, sources);
    }

    /**
     * Imports rules using the specified DSL provider and rule sources into this ruleset builder.
     *
     * @param dslName The name of the DSL provider.
     * @param sources Data sources that may be needed for loading the rules.
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @throws IllegalStateException    if no or multiple providers were found for the specified DSL identifier.
     * @see DSLKnowledgeProvider#appendTo(RuleSetBuilder, java.util.function.Predicate, Object[]) for more details
     */
    @SuppressWarnings("unchecked")
    default <S> RuleSetBuilder<C> importAllRules(@NonNull String dslName, S... sources) throws IOException {
        return importAllRules(DSLKnowledgeProvider.load(dslName), sources);
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
