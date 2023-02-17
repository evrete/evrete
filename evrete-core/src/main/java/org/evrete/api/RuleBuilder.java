package org.evrete.api;

import java.util.function.Predicate;

public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, LhsFactSelector<LhsBuilder<C>> {

    LhsBuilder<C> getLhs();

    RuleBuilder<C> salience(int salience);

    <Z> RuleBuilder<C> property(String property, Z value);

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
     */
    EvaluatorHandle createCondition(String expression, double complexity);

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
     */
    default EvaluatorHandle createCondition(String expression) {
        return createCondition(expression, WorkUnit.DEFAULT_COMPLEXITY);
    }


}
