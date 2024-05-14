package org.evrete.api.builders;

import org.evrete.api.*;
import org.evrete.util.CompilationException;

import java.util.function.Predicate;

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
     * Sets a property of the RuleBuilder instance.
     *
     * @param <Z>      the type of the property value
     * @param property the name of the property
     * @param value    the value of the property
     * @return a RuleBuilder instance
     * @deprecated use {@link #set(String, Object)} instead
     */
    @Deprecated
    default <Z> RuleBuilder<C> property(String property, Z value) {
        return set(property, value);
    }

    /**
     * Returns the runtime context of this rule builder.
     *
     * @return the runtime object
     */
    C getRuntime();


    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    default EvaluatorHandle createCondition(ValuesPredicate predicate, FieldReference... references) {
        return createCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    default EvaluatorHandle createCondition(ValuesPredicate predicate, String... references) {
        return createCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or used in the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace existing conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    default EvaluatorHandle createCondition(Predicate<Object[]> predicate, FieldReference... references) {
        return createCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    default EvaluatorHandle createCondition(Predicate<Object[]> predicate, String... references) {
        return createCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * <p>
     * Compiles and registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param expression literal condition
     * @param complexity condition's relative complexity
     * @return evaluator handle
     * @see WorkUnit
     * @throws CompilationException if the engine fails to compile the expression
     */
    EvaluatorHandle createCondition(String expression, double complexity) throws CompilationException;

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param complexity condition's relative complexity
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    EvaluatorHandle createCondition(ValuesPredicate predicate, double complexity, String... references);

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param complexity condition's relative complexity
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    EvaluatorHandle createCondition(Predicate<Object[]> predicate, double complexity, String... references);

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param complexity condition's relative complexity
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    EvaluatorHandle createCondition(ValuesPredicate predicate, double complexity, FieldReference... references);

    /**
     * <p>
     * Registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param predicate  condition predicate
     * @param complexity condition's relative complexity
     * @param references field references
     * @return evaluator handle
     * @see FieldReference
     * @see WorkUnit
     */
    EvaluatorHandle createCondition(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    /**
     * <p>
     * Compiles and registers the provided predicate with the current context (a {@link Knowledge} or a {@link RuleSession}).
     * The resulting {@link EvaluatorHandle} can later be used in the {@link LhsBuilder#where(EvaluatorHandle...)}
     * code blocks, shared among other rules (if appropriate), or provided to the
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, Evaluator)} and
     * {@link EvaluatorsContext#replaceEvaluator(EvaluatorHandle, ValuesPredicate)} methods to replace conditions
     * on the fly.
     * </p>
     *
     * @param expression literal condition
     * @return evaluator handle
     * @see WorkUnit
     * @throws CompilationException if the engine fails to compile the expression
     */
    default EvaluatorHandle createCondition(String expression) throws CompilationException {
        return createCondition(expression, WorkUnit.DEFAULT_COMPLEXITY);
    }
}
