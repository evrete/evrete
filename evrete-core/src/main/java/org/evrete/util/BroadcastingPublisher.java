package org.evrete.util;

import org.evrete.api.events.Events;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.evrete.util.CommonUtils.newIdentityHashSet;

/**
 * An implementation of the {@link Events.Publisher} interface that can broadcast events.
 * When constructed using the {@link #BroadcastingPublisher(BroadcastingPublisher)} constructor,
 * this implementation will publish events to both its own subscriptions and those of its parent.
 *
 * @param <E> the type of event that this publisher will broadcast
 */
public class BroadcastingPublisher<E extends Events.Event> implements Events.Publisher<E> {
    private final Hierarchy<SplitSubscriptions<E>> innerSubscriptions;
    private final Executor executor;


    private BroadcastingPublisher(Hierarchy<SplitSubscriptions<E>> innerSubscriptions, Executor executor) {
        this.innerSubscriptions = innerSubscriptions;
        this.executor = executor;
    }

    public BroadcastingPublisher(Executor executor) {
        this(new Hierarchy<>(new SplitSubscriptions<>()), executor);
    }

    public BroadcastingPublisher(BroadcastingPublisher<E> other) {
        this(new Hierarchy<>(new SplitSubscriptions<>(), other.innerSubscriptions), other.executor);
    }

    //TODO rename
    public int totalLocalSubscriptions() {
        return innerSubscriptions.getValue().size();
    }

    @Override
    public synchronized Events.Subscription subscribe(boolean async, Consumer<E> listener) {
        InnerSubscription<E> result = new InnerSubscription<>(this, async, listener);
        innerSubscriptions.getValue().add(result);
        return result;
    }

    public void broadcast(E event) {
        this.innerSubscriptions.walkUp(subscriptions -> subscriptions.broadcast(event, executor));
    }

    private void removeSubscription(InnerSubscription<E> subscription) {
        this.innerSubscriptions.getValue().removeSubscription(subscription);
    }

    /**
     * An internal implementation of the {@link Events.Subscription} API
     * @param <P>
     */
    private static class InnerSubscription<P extends Events.Event> implements Events.Subscription {
        private final BroadcastingPublisher<P> publisher;
        private final boolean async;
        private final Consumer<P> action;
        private boolean cancelled = false;

        InnerSubscription(BroadcastingPublisher<P> publisher, boolean async, Consumer<P> action) {
            this.publisher = publisher;
            this.async = async;
            this.action = action;
        }

        @Override
        public synchronized void cancel() {
            this.cancelled = true;
            this.publisher.removeSubscription(this);
        }
    }

    /**
     * A convenience class for storing both sync and async subscriptions in one place.
     * @param <H> type of events
     */
    private static class SplitSubscriptions<H extends Events.Event> {
        private final Collection<InnerSubscription<H>> subscriptionsSync = newIdentityHashSet();
        private final Collection<InnerSubscription<H>> subscriptionsAsync = newIdentityHashSet();

        void add(InnerSubscription<H> subscription) {
            if(subscription.async) {
                this.subscriptionsAsync.add(subscription);
            } else {
                this.subscriptionsSync.add(subscription);
            }
        }

        private void removeSubscription(InnerSubscription<H> subscription) {
            if(subscription.async) {
                this.subscriptionsAsync.remove(subscription);
            } else {
                this.subscriptionsSync.remove(subscription);
            }
        }

        int size() {
            return subscriptionsSync.size() + subscriptionsAsync.size();
        }

        void broadcast(H event, Executor executor) {
            // 1. Scan async subscriptions first
            for (InnerSubscription<H> s : subscriptionsAsync) {
                if(!s.cancelled) {
                    executor.execute(() -> s.action.accept(event));
                }
            }

            // 2. Scan the rest
            for (InnerSubscription<H> s : subscriptionsSync) {
                if (!s.cancelled) {
                    s.action.accept(event);
                }
            }
        }
    }
}
