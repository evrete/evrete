package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.events.ConditionEvaluationEvent;

import java.util.function.BiConsumer;

/**
 * A context that maintains a collection of every condition used in the engine's runtime.
 * This interface allows developers to register an {@link Evaluator} and use its
 * {@link EvaluatorHandle} to later update the supplied condition or subscribe to condition evaluation events.
 * <p>
 * The context adheres to the context separation principle, i.e., changes made to a
 * {@link Knowledge}-level context are propagated down to each spawned {@link RuleSession}, but not vice versa.
 * </p>
 */
public interface EvaluatorsContext {

    /**
     * <p>
     * Returns evaluator by its handle.
     * </p>
     *
     * @param handle evaluator handle
     * @return existing condition predicate or null if such condition does not exist
     */
    ValuesPredicate getPredicate(EvaluatorHandle handle);


    /**
     * Creates or returns an existing publisher for a condition with the provided handle.
     *
     * @param handle the evaluator handle
     * @return a publisher of evaluation results
     */
    @NonNull
    Events.Publisher<ConditionEvaluationEvent> publisher(EvaluatorHandle handle);

    /**
     * Executes the specified action for each Evaluator with its corresponding handle.
     *
     * @param listener The action to be performed.
     */
    void forEach(BiConsumer<EvaluatorHandle, ValuesPredicate> listener);


    /**
     * Replaces an existing condition with a new one. This method does not check the signature which
     * may result in unpredictable results when applied incorrectly.
     *
     * @param handle       the handle of an existing condition
     * @param newPredicate the new condition in the form of a {@link ValuesPredicate}
     */
    void replacePredicate(EvaluatorHandle handle, ValuesPredicate newPredicate);

}
