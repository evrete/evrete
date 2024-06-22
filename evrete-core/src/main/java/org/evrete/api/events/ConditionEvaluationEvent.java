package org.evrete.api.events;

import org.evrete.api.IntToValue;
import org.evrete.api.RuleSession;
import org.evrete.api.ValuesPredicate;

/**
 * Represents an event that occurs when a condition is evaluated.
 *
 * @see ContextEvent
 */
public interface ConditionEvaluationEvent extends TimedEvent {

    /**
     * Returns the evaluator responsible for the condition evaluation.
     *
     * @return the evaluator
     */
    ValuesPredicate getCondition();

    /**
     * Returns the values involved in the condition evaluation.
     *
     * @return an instance of {@link IntToValue} representing the values
     */
    Object[] getArguments();

    /**
     * Returns the result of the condition evaluation.
     *
     * @return {@code true} if the condition is met, {@code false} otherwise
     */
    boolean isPassed();

    /**
     * Returns the {@link RuleSession} during which the condition was evaluated.
     *
     * @return the session context
     */
    RuleSession<?> getContext();
}
