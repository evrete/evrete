package org.evrete.api.events;

import org.evrete.KnowledgeService;
import org.evrete.api.Events;

import java.util.function.Consumer;

/**
 * Interface representing an Event Bus for subscribing to events.
 */
public interface EventBus {
    /**
     * Subscribes an even consumer to a specific event class type.
     *
     * <p>When an event of the specified type is published, the consumer will be invoked.</p>
     * <p>The consumer can be notified asynchronously based on the {@code async} parameter.</p>
     *
     * @param <E>        the type of the event to listen for
     * @param eventClass the class object of the event type
     * @param async      if {@code true}, the consumer will be invoked asynchronously using the
     *                   {@link KnowledgeService#getExecutor()}; if {@code false}, it will be invoked synchronously
     * @param consumer   the consumer that will handle events of the specified type
     * @return a {@link Events.Subscription} that can be used to unsubscribe the consumer from the event type
     * @throws IllegalArgumentException if the provided event class is not supported
     * @see ContextEvent
     * @see Events.Publisher
     * @see Events.Subscription
     */
    default <E extends ContextEvent> Events.Subscription subscribe(Class<E> eventClass, boolean async, Consumer<E> consumer) {
        return getPublisher(eventClass).subscribe(async, consumer);
    }

    /**
     * Returns a publisher for a specific event class type.
     *
     * @param <E> the type of the event to publish
     * @param eventClass the class object of the event type
     * @return an {@link Events.Publisher} for the specified event class type
     * @throws IllegalArgumentException if the provided event class is not supported
     * @see ContextEvent
     * @see Events.Publisher
     */
    <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass);

    default <E extends ContextEvent> Events.Subscription subscribe(Class<E> eventClass, Consumer<E> consumer) {
        return subscribe(eventClass, false, consumer);
    }

    default <E extends ContextEvent> Events.Subscription subscribeAsync(Class<E> eventClass, Consumer<E> consumer) {
        return subscribe(eventClass, true, consumer);
    }

}
