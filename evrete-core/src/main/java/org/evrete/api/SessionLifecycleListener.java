package org.evrete.api;

import java.util.EventListener;
import java.util.function.Consumer;

/**
 * An interface that defines the methods to be implemented by
 * classes that wish to listen for session lifecycle events.
 *
 * @deprecated since 4.0.0. The library has moved from an Observer to a PubSub pattern.
 * See the {@link RuntimeContext#subscribe(Class, boolean, Consumer)} method for alternatives to adding listeners.
 */
@Deprecated
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
