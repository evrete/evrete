package org.evrete.api;

/**
 * Interface for managing {@code EvaluationListener} instances.
 * Allows adding and removing listeners to handle evaluation events.
 */
public interface EvaluationListeners {

    /**
     * Adds a listener to be notified of evaluation events.
     * If the listener is already added, behavior depends on the implementation.
     *
     * @param listener the listener to add, not {@code null}.
     */
    void addListener(EvaluationListener listener);

    /**
     * Removes a previously added listener. If the listener is not registered,
     * this method has no effect.
     *
     * @param listener the listener to remove, not {@code null}.
     */
    void removeListener(EvaluationListener listener);
}
