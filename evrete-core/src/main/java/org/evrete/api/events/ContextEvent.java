package org.evrete.api.events;

import org.evrete.api.Events;

import java.util.function.Consumer;

/**
 * A marker interface for all {@link org.evrete.api.RuntimeContext} events
 *
 * <p>Currently supported event types:
 * <ul>
 *     <li>{@link SessionCreatedEvent}</li>
 *     <li>{@link KnowledgeCreatedEvent}</li>
 *     <li>{@link ConditionEvaluationEvent}</li>
 * </ul>
 * </p>
 *
 * @see Events.Subscription
 * @see org.evrete.api.RuntimeContext#subscribe(Class, boolean, Consumer)
 */
public interface ContextEvent extends Events.Event{
    //TODO document all supported events

}