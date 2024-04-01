package org.evrete.api;

import java.util.EventListener;

/**
 * An interface that defines the methods to be implemented by
 * classes that wish to listen for session lifecycle events.
 *
 * @see SessionLifecycleListener.Event for the types of events.
 */
public interface SessionLifecycleListener extends EventListener {

    /**
     * Invoked when a session lifecycle event occurs.
     *
     * @param evt the event that occurred
     */
    void onEvent(Event evt);

    /**
     * Enumeration of the different events that can trigger the callback.
     */
    enum Event {
        /**
         * Event indicating that a session is about to fire.
         */
        PRE_FIRE,
        /**
         * Event indicating that a session is about to close.
         */
        PRE_CLOSE
    }
}
