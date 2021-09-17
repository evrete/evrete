package org.evrete.api;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>
 * Unlike stateful sessions, stateless sessions are designed to be short-living instances
 * that can be fired only once, returning the resulting working memory snapshot.
 * Generally, every {@link StatelessSession} can be considered as a {@link StatefulSession}
 * that automatically calls {@link StatefulSession#close()} after {@link StatefulSession#fire()}.
 * </p>
 */
public interface StatelessSession extends RuleSession<StatelessSession> {


    /**
     * <p>
     * Fires the session and calls the consumer for each memory object and its fact handle.
     * </p>
     *
     * @param consumer consumer for session memory
     */
    void fire(BiConsumer<FactHandle, Object> consumer);

    /**
     * <p>
     * Fires the session and calls the consumer for each memory object.
     * </p>
     *
     * @param consumer consumer for session memory
     */
    void fire(Consumer<Object> consumer);

    /**
     * <p>
     * A convenience method to retrieve facts of a specific type name.
     * </p>
     *
     * @param consumer consumer for session memory
     */
    <T> void fire(String type, Consumer<T> consumer);

    /**
     * <p>
     * A convenience method to retrieve the resulting instances of a specific Java class.
     * </p>
     *
     * @param consumer consumer for session memory
     */
    <T> void fire(Class<T> type, Consumer<T> consumer);
}
