package org.evrete.api;

/**
 * <p>
 * A collection of every condition tha was ever used in the engine's runtime. It allows developers
 * to register an {@link Evaluator} and use its {@link EvaluatorHandle} to later update the supplied
 * condition.
 * </p>
 */
public interface EvaluatorsContext {

    /**
     * <p>
     *     Registers new condition evaluator and returns its handle. If an existing {@link Evaluator}
     *     matches the argument ({@link Evaluator#compare(Evaluator)} returns {@link Evaluator#RELATION_EQUALS}),
     *     the existing evaluator handle will be returned instead and no changes will be made in the
     *     context.
     * </p>
     * @param evaluator condition to add
     * @param complexity condition's relative complexity
     * @return new {@link EvaluatorHandle} or the one of an existing condition.
     */
    EvaluatorHandle addEvaluator(Evaluator evaluator, double complexity);

    /**
     * <p>
     *     Returns evaluator by its handle.
     * </p>
     * @param handle evaluator handle
     * @return existing condition evaluator or null if such condition does not exist
     */
    Evaluator getEvaluator(EvaluatorHandle handle);

    /**
     *
     * @param evaluator condition to add
     * @see #addEvaluator(Evaluator, double)
     * @return new {@link EvaluatorHandle} or the one of an existing condition.
     */
    default EvaluatorHandle addEvaluator(Evaluator evaluator) {
        return addEvaluator(evaluator, EvaluatorHandle.DEFAULT_COMPLEXITY);
    }

    /**
     * <p>
     *     Replaces existing condition with a new one. New condition must have
     *     the same {@link Evaluator#descriptor()}, otherwise {@link IllegalArgumentException}
     *     will be thrown.
     * </p>
     * @param handle handle of an existing condition
     * @throws IllegalArgumentException if no condition can be found by the given handle or
     * if the existing condition's descriptor does not match the new one.
     * @param newEvaluator new condition
     */
    void replaceEvaluator(EvaluatorHandle handle, Evaluator newEvaluator);

    /**
     * <p>
     *     Replaces existing condition with a new one.
     * </p>
     * @param handle handle of an existing condition
     * @param predicate new condition in a form if {@link ValuesPredicate}
     */
    void replaceEvaluator(EvaluatorHandle handle, ValuesPredicate predicate);

}
