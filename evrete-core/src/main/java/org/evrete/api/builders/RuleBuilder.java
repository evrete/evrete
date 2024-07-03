package org.evrete.api.builders;

import org.evrete.api.*;

import java.util.function.Consumer;

/**
 * The RuleBuilder interface represents a builder for creating rules.
 *
 * @param <C> the type of the {@link RuntimeContext} of the builder
 */
public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, LhsFactSelector<LhsBuilder<C>>, FluentEnvironment<RuleBuilder<C>> {

    /**
     * Returns the LhsBuilder instance.
     *
     * @return the LhsBuilder instance
     */
    LhsBuilder<C> getLhs();

    /**
     * Sets the salience of the rule.
     *
     * @param salience the salience value of the rule
     * @return a RuleBuilder instance
     */
    RuleBuilder<C> salience(int salience);


    /**
     * Returns the runtime context of this rule builder.
     *
     * @return the runtime object
     */
    C getRuntime();


    /**
     * <p>
     * Terminates current rule builder as a rule without action.
     * </p>
     *
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute();

    /**
     * <p>
     * Terminates current rule builder with the provided RHS action
     * </p>
     *
     * @param consumer RHS
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute(Consumer<RhsContext> consumer);


    /**
     * <p>
     * Terminates current rule builder with the provided RHS action
     * </p>
     *
     * @param literalRhs RHS action as Java code
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute(String literalRhs);


    /**
     * Returns a non-fluent API for adding conditions and obtain their {@link EvaluatorHandle} handles.
     * @see ConditionManager
     * @see EvaluatorHandle
     * @see EvaluatorsContext
     */
    ConditionManager getConditionManager();

}
