package org.evrete.api.builders;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.EvaluatorsContext;
import org.evrete.api.ValuesPredicate;
import org.evrete.api.WorkUnit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Previously part of the {@link RuleBuilder} interface, since version 4.0.0, this interface contains methods
 * for declaring various rule-level conditions and obtaining their references. These references can later be used
 * in the {@link EvaluatorsContext} methods to replace existing conditions on the fly or to subscribe to
 * evaluation events.
 * <p>
 * If you don't need these condition handles, it is better to use the standard
 * <code>where(...)</code> methods of {@link LhsBuilder}, which provide the same functionality,
 * and offer a more fluent API compared to this interface.
 * </p>
 */
public interface ConditionManager {

    /**
     * Adds the provided predicate to the current {@link RuleBuilder} as a new condition.
     * The resulting {@link EvaluatorHandle} can later be provided to the
     * {@link EvaluatorsContext#replacePredicate(EvaluatorHandle, ValuesPredicate)} method to replace conditions
     * on the fly or subscribe to evaluation events.
     * <p>
     * The provided references must be of the same order and type as expected by the predicate argument.
     * </p>
     *
     * @param predicate  the condition predicate
     * @param complexity the condition's relative complexity
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>
     * @return the evaluator handle
     * @see WorkUnit
     */
    EvaluatorHandle addCondition(ValuesPredicate predicate, double complexity, String... references);

    /**
     * Adds the provided predicate to the current {@link RuleBuilder} as a new condition.
     * The resulting {@link EvaluatorHandle} can later be provided to the
     * {@link EvaluatorsContext#replacePredicate(EvaluatorHandle, ValuesPredicate)} method to replace conditions
     * on the fly or subscribe to evaluation events.
     * <p>
     * The provided references must be of the same order and type as expected by the predicate argument.
     * </p>
     *
     * @param predicate  the condition predicate
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>
     * @return the evaluator handle
     * @see #addCondition(ValuesPredicate, double, String...)
     */
    default EvaluatorHandle addCondition(ValuesPredicate predicate, String... references) {
        return addCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * Adds the provided predicate to the current {@link RuleBuilder} as a new condition.
     * The resulting {@link EvaluatorHandle} can later be provided to the
     * {@link EvaluatorsContext#replacePredicate(EvaluatorHandle, ValuesPredicate)} method to replace conditions
     * on the fly or subscribe to evaluation events.
     * <p>
     * The provided references must be of the same order and type as expected by the predicate argument.
     * </p>
     *
     * @param predicate  the condition predicate
     * @param complexity the condition's relative complexity
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>
     * @return the evaluator handle
     * @see WorkUnit
     */
    EvaluatorHandle addCondition(Predicate<Object[]> predicate, double complexity, String... references);

    /**
     * Adds the provided predicate to the current {@link RuleBuilder} as a new condition.
     * The resulting {@link EvaluatorHandle} can later be provided to the
     * {@link EvaluatorsContext#replacePredicate(EvaluatorHandle, ValuesPredicate)} method to replace conditions
     * on the fly or subscribe to evaluation events.
     * <p>
     * The provided references must be of the same order and type as expected by the predicate argument.
     * </p>
     *
     * @param predicate  the condition predicate
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>
     * @return the evaluator handle
     * @see #addCondition(Predicate, double, String...)
     */
    default EvaluatorHandle addCondition(Predicate<Object[]> predicate, String... references) {
        return addCondition(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * Compiles and adds the provided literal predicate to the current {@link RuleBuilder} as a new condition.
     * Unlike other <code>addCondition</code> methods, this method returns a {@link CompletableFuture} which
     * will be available after the {@link RuleSetBuilder#build()} method is called. The resulting
     * {@link EvaluatorHandle} can later be provided to the
     * {@link EvaluatorsContext#replacePredicate(EvaluatorHandle, ValuesPredicate)} method to replace
     * conditions on the fly or subscribe to evaluation events.
     *
     * @param expression the literal condition, e.g., <code>$a.value > $c.code.value</code>
     * @param complexity the condition's relative complexity
     * @return a {@link CompletableFuture} representing the {@link EvaluatorHandle} that will be available when
     * the current ruleset is built.
     * @see WorkUnit
     */
    CompletableFuture<EvaluatorHandle> addCondition(String expression, double complexity);

}
