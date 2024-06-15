package org.evrete.runtime.events;

import org.evrete.api.Events;

import java.util.function.Supplier;

public class SingleSubscriberPublisher<T> implements Publisher<T> {
    private Subscriber<? super T> subscriber;
    private InnerSubscription subscription;

    @Override
    public Events.Subscription subscribe(Subscriber<? super T> subscriber) {
        if(this.subscription == null) {
            return (this.subscription = new InnerSubscription(subscriber));
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    protected final void publishIfSubscribed(Supplier<T> eventSupplier) {
        if(subscription != null) {
            subscription.subscriber.onNext(eventSupplier.get());
        }
    }

    private void removeSubscription() {
        this.subscription = null;
    }

    private class InnerSubscription implements Events.Subscription {
        private final Subscriber<? super T> subscriber;

        InnerSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            removeSubscription();
        }
    }

}
