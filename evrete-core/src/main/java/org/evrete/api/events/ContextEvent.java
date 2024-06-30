package org.evrete.api.events;

import java.util.function.Consumer;

/**
 * A marker interface for all {@link org.evrete.api.RuntimeContext} events
 *
 * <p>Currently supported event types:
 * <ul>
 *     <li>{@link SessionCreatedEvent}</li>
 *     <li>{@link SessionFireEvent}</li>
 *     <li>{@link SessionClosedEvent}</li>
 *     <li>{@link KnowledgeCreatedEvent}</li>
 *     <li>{@link ConditionEvaluationEvent}</li>
 *     <li>{@link EnvironmentChangeEvent}</li>
 * </ul>
 * </p>
 *
 * @see Events.Subscription
 * @see org.evrete.api.RuntimeContext#subscribe(Class, boolean, Consumer)
 */
public interface ContextEvent extends Events.Event{

}
