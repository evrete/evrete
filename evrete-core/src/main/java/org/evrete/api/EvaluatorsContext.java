package org.evrete.api;

/**
 * <p>
 * A collection of every condition tha was ever used in the engine's runtime. It allows developers
 * to register an {@link Evaluator} and use its {@link EvaluatorHandle} to later update the supplied
 * condition.
 * </p>
 */
public interface EvaluatorsContext {

    EvaluatorHandle addEvaluator(Evaluator evaluator, double complexity);

    default EvaluatorHandle addEvaluator(Evaluator evaluator) {
        return addEvaluator(evaluator, EvaluatorHandle.DEFAULT_COMPLEXITY);
    }


    void replaceEvaluator(EvaluatorHandle handle, Evaluator newEvaluator);
}
