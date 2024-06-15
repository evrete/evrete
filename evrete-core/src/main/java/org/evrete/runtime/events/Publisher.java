package org.evrete.runtime.events;

import org.evrete.api.Events;

/**
 * Interface for publishing items to subscribers.
 *
 * @param <T> the type of items produced by the publisher
 */
public interface Publisher<T> {

    /**
     * Adds the given subscriber to this publisher and return the subscription handle
     *
     * @param subscriber the subscriber to receive items
     */
    Events.Subscription subscribe(Subscriber<? super T> subscriber);
}
