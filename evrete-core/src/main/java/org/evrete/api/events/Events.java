package org.evrete.api.events;

import org.evrete.KnowledgeService;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;

/**
 * A simpler alternative to the {@link java.util.concurrent.Flow} class and its interfaces.
 * It uses consumers instead of subscribers and allows both synchronous and asynchronous subscriptions.
 */
public final class Events {

    private Events() {}

    /**
     * A Publisher provides a mechanism to subscribe consumers to it.
     *
     * @param <E> the type of event.
     */
    public interface Publisher<E extends Event> {
        /**
         * Subscribes a consumer to this publisher.
         *
         * @param async    if {@code true}, the listener will be invoked asynchronously using the
         *                 {@link KnowledgeService#getExecutor()}; if {@code false}, it will be invoked synchronously
         * @param listener the consumer that will receive the published items.
         * @return the result of the subscribe operation.
         */
        Subscription subscribe(boolean async, Consumer<E> listener);

        /**
         * Subscribes a consumer to this publisher and stores the subscription in the provided cancellable collection.
         *
         * @param async    if {@code true}, the listener will be invoked asynchronously using the
         *                 {@link KnowledgeService#getExecutor()}; if {@code false}, it will be invoked synchronously
         * @param listener the consumer that will receive the published items.
         */
        default void subscribe(Subscriptions sink, boolean async, Consumer<E> listener) {
            sink.add(subscribe(async, listener));
        }

        default Subscription subscribe(Consumer<E> listener) {
            return subscribe(false, listener);
        }

        default void subscribe(Subscriptions sink, Consumer<E> listener) {
            sink.add(subscribe(listener));
        }

        default Subscription subscribeAsync(Consumer<E> listener) {
            return subscribe(true, listener);
        }

        default void subscribeAsync(Subscriptions sink, Consumer<E> listener) {
            sink.add(subscribeAsync(listener));
        }
    }

    /**
     * Interface for controlling the subscription.
     */
    public interface Subscription {
        /**
         * Cancel the subscription, indicating that no more items are required.
         */
        void cancel();
    }

    /**
     * A marker interface for all events.
     */
    public interface Event {

    }

    /**
     * Utility class for cancelling multiple {@link Subscription} objects at once.
     */
    public static class Subscriptions {

        private final Set<Subscription> subscriptions = synchronizedSet(newSetFromMap(new IdentityHashMap<>()));

        /**
         * Adds a {@code Subscription} to this utility.
         *
         * @param subscription the {@code Subscription} to add
         * @return the current {@code Subscriptions} instance
         */
        public Subscriptions add(Subscription subscription) {
            this.subscriptions.add(subscription);
            return this;
        }

        /**
         * Cancels all added subscriptions and removes them from the collection.
         */
        public void cancel() {
            synchronized (subscriptions) {
                Iterator<Subscription> iterator = subscriptions.iterator();
                while (iterator.hasNext()) {
                    iterator.next().cancel();
                    iterator.remove();
                }
            }
        }
    }
}
