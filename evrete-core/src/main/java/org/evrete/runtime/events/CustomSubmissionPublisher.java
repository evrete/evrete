package org.evrete.runtime.events;

import org.evrete.api.Events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class CustomSubmissionPublisher<T> implements Publisher<T>, AutoCloseable {
    private final Collection<InnerSubscription> subscriptions = new ArrayList<>();
    protected final ExecutorService executor;
    private boolean closed = false;

    public CustomSubmissionPublisher(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public synchronized Events.Subscription subscribe(Subscriber<? super T> subscriber) {
        if (closed) {
            throw new IllegalStateException("Publisher already closed");
        }
        InnerSubscription subscription = new InnerSubscription(subscriber, this);
        subscriptions.add(subscription);
        return subscription;
    }

    //@Override
    public synchronized Events.Subscription subscribe(Consumer<? super T> consumer) {
        return this.subscribe(toSubscriber(consumer));
    }

    public void publish(T item) {
        if (closed) {
            throw new IllegalStateException("Publisher already closed");
        }
        for (InnerSubscription subscription : subscriptions) {
            if (!subscription.cancelled) {
                if(executor == null) {
                    subscription.subscriber.onNext(item);
                } else {
                    executor.execute(() -> subscription.subscriber.onNext(item));
                }
            }
        }
    }

    void removeSubscription(InnerSubscription subscription) {
        subscriptions.remove(subscription);
    }

    @Override
    public synchronized void close() {
        closed = true;
        subscriptions.clear();
    }

    Subscriber<T> toSubscriber(Consumer<? super T> consumer) {
        return consumer::accept;
    }


    private class InnerSubscription implements Events.Subscription {
        private final Subscriber<? super T> subscriber;
        private final CustomSubmissionPublisher<T> publisher;
        private volatile boolean cancelled = false;

        InnerSubscription(Subscriber<? super T> subscriber, CustomSubmissionPublisher<T> publisher) {
            this.subscriber = subscriber;
            this.publisher = publisher;
        }

        @Override
        public void cancel() {
            cancelled = true;
            publisher.removeSubscription(this);
        }
    }
}


