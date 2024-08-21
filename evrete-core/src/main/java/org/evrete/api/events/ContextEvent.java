package org.evrete.api.events;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.RuntimeContext;

import java.util.function.Consumer;

/**
 * A marker interface for all {@link org.evrete.api.RuntimeContext} events.
 * <p>Note that starting with version 4.0.0, the {@link ConditionEvaluationEvent} is no longer
 * considered a context-wide event. Instead, it should be subscribed to via the
 * {@link org.evrete.api.EvaluatorsContext#publisher(EvaluatorHandle)} method.
 * </p>
 *
 * <p>Currently supported event types:</p>
 * <ul>
 *     <li>{@link SessionCreatedEvent}</li>
 *     <li>{@link SessionFireEvent}</li>
 *     <li>{@link SessionClosedEvent}</li>
 *     <li>{@link KnowledgeCreatedEvent}</li>
 *     <li>{@link EnvironmentChangeEvent}</li>
 * </ul>
 *
 * @see Events.Subscription
 * @see org.evrete.api.RuntimeContext#subscribe(Class, boolean, Consumer)
 * @see RuntimeContext#getEvaluatorsContext()
 */
public interface ContextEvent extends Events.Event{

}
