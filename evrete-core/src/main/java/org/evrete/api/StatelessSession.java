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
     * Fires the session and performs no memory scan. This method might be useful if developer is
     * holding references to fact variables and only interested in changes of those facts:
     * </p>
     * <pre>{@code
     * Customer c = new Customer();
     * session.insert(c);
     * session.fire();
     * System.out.println(c.getSomeUpdatedProperty());
     * }</pre>
     * <p>
     * While this method is the fastest among the other {@code fire(..)} methods, it is only
     * applicable if the provided {@link FactStorage} SPI implementation stores facts by reference
     * (e.g. does not serialize/deserialize objects)
     * </p>
     */
    void fire();

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


    default void insertAndFire(Object... objects) {
        insert(objects);
        fire();
    }
}
