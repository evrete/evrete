package org.evrete.api;

import java.util.EventListener;

/**
 * The {@code EvaluationListener} interface should be implemented by
 * classes that wish to receive notifications of evaluation results.
 * This interface extends from {@link java.util.EventListener},
 * marking it as a specialized listener for handling evaluation-related events.
 * Implementors of this interface should override the {@code fire()} method
 * to define custom handling behavior for evaluation results.
 *
 * @see java.util.EventListener
 */
public interface EvaluationListener extends EventListener {

    /**
     * Invoked when an evaluation event occurs.
     * <p>
     * This method is called to notify the listener of the result of an
     * evaluation. Implementing classes should provide their own implementation
     * of how to handle this event, which might include processing the evaluation
     * result or updating the state of other components based on the evaluation.
     *
     * @param evaluator The {@link Evaluator} instance that performed the evaluation.
     * @param values    The {@link  IntToValue} instance associated with the evaluation.
     *                  This provides a mapping or association used during the evaluation,
     *                  which can be essential for understanding how the evaluation result
     *                  was derived.
     * @param result    A boolean representing the result of the evaluation.
     */
    void fire(Evaluator evaluator, IntToValue values, boolean result);

}
