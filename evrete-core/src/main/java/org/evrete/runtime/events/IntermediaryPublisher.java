package org.evrete.runtime.events;

import org.evrete.api.Events;

import java.util.function.Consumer;

public class IntermediaryPublisher<T> extends CustomSubmissionPublisher<T> {
    private final MetaStorage metaStorage = new MetaStorage();

    public IntermediaryPublisher(CustomSubmissionPublisher<T> other) {
        super(other.executor);
    }

    public void addPublisher(CustomSubmissionPublisher<T> publisher) {
        metaStorage.onNewPublisher(publisher);
        publisher.subscribe(new IntermediarySubscriber());
    }

    @Override
    public synchronized Events.Subscription subscribe(Subscriber<? super T> subscriber) {
        metaStorage.onNewSubscriber(subscriber);
        return super.subscribe(subscriber);
    }

    public synchronized Events.Subscription subscribe(Consumer<? super T> consumer) {
        return this.subscribe(toSubscriber(consumer));
    }

    /**
     * A storage for child publishers and subscribers. We may need it if we extend
     * the inner subscriber interface. For now, it's effectively just a consumer without
     * onSubscribe(), onClose(), and onError() callbacks.
     */
    private class MetaStorage {
        private void onNewSubscriber(Subscriber<? super T> ignored) {
        }

        private void onNewPublisher(CustomSubmissionPublisher<T> ignored) {
        }
    }

    private class IntermediarySubscriber implements Subscriber<T> {

        @Override
        public void onNext(T item) {
            publish(item);
        }
    }
}
