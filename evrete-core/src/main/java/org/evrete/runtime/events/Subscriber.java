package org.evrete.runtime.events;

/**
 * Interface for receiving items from a publisher.
 *
 * @param <T> the type of items received by the subscriber
 */
@FunctionalInterface
public interface Subscriber<T>  {

    void onNext(T t);
}
