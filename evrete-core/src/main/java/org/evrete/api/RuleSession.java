package org.evrete.api;

import java.util.function.Consumer;
import java.util.stream.Collector;

/**
 * <p>
 * Base interface for both stateful and stateless sessions
 * </p>
 */
public interface RuleSession<S extends RuleSession<S>> extends RuleSetContext<S, RuntimeRule>, SessionOps, MemoryStreaming {



    /**
     * Returns fact by its handle.
     *
     * @param handle fact handle
     * @param <T>    type of the fact (use {@code Object}  or wildcard if type is unknown)
     * @return fact or {@code null} if fact is not found
     */
    <T> T getFact(FactHandle handle);

    /**
     * A convenience method that returns an instance of {@link Collector} for inserting
     * streams of facts.
     *
     * @param <T> the type of input elements to the reduction operation
     * @return collector
     */
    <T> Collector<T, ?, S> asCollector();



    /**
     * <p>
     * Inserts a collection of facts, marking them as being of a specific type.
     * </p>
     *
     * @param type    type name
     * @param objects objects to insert
     * @return this instance
     * @see #insertAs(String, Object)
     */
    //TODO create test
    @SuppressWarnings("unchecked")
    default S insertAs(String type, Iterable<?> objects) {
        insert0(type, objects, true);
        return (S) this;
    }

    /**
     * <p>
     * Inserts a collection of facts, marking them as being of a specific type.
     * </p>
     *
     * @param type    type name
     * @param objects objects to insert
     * @return this instance
     * @see #insertAs(String, Object)
     */
    @SuppressWarnings("unchecked")
    default S insertAs(String type, Object... objects) {
        insert0(type, objects, true);
        return (S) this;
    }

    /**
     * <p>
     * Inserts a collection of facts.
     * </p>
     *
     * @param objects objects to insert
     * @return this instance
     * @see #insert(Object)
     */
    @SuppressWarnings("unchecked")
    default S insert(Iterable<?> objects) {
        insert0(objects, true);
        return (S) this;
    }

    /**
     * <p>
     * Inserts an array of facts.
     * </p>
     *
     * @param objects objects to insert
     * @return this instance
     * @see #insert(Object)
     */
    @SuppressWarnings("unchecked")
    default S insert(Object... objects) {
        insert0(objects, true);
        return (S) this;
    }

    /**
     * Retrieves the activation manager associated with this session.
     *
     * @return the activation manager for this session
     */
    ActivationManager getActivationManager();

    /**
     * Sets the activation manager for the session.
     *
     * @param activationManager the activation manager to set
     * @return the current session instance
     */
    S setActivationManager(ActivationManager activationManager);

    /**
     * Adds a session lifecycle listener to receive session lifecycle events.
     *
     * @param listener the session lifecycle listener to add
     * @return the current instance of the session
     * @deprecated since 4.0.0. The library has moved from an Observer to a PubSub pattern.
     * See the {@link RuntimeContext#subscribe(Class, boolean, Consumer)} method for alternatives to adding listeners.
     */
    @Deprecated
    default S addEventListener(SessionLifecycleListener listener) {
        throw new UnsupportedOperationException("The library has moved from an Observer to a PubSub pattern. See the migration guides.");
    }

    /**
     * Removes a {@link SessionLifecycleListener} from the session.
     *
     * @param listener the {@link SessionLifecycleListener} to be removed
     * @return the current instance of the session
     * @deprecated since 4.0.0. The library has moved from an Observer to a PubSub pattern.
     * See the {@link RuntimeContext#subscribe(Class, boolean, Consumer)} method for alternatives to adding listeners.
     */
    @Deprecated
    default S removeEventListener(SessionLifecycleListener listener) {
        throw new UnsupportedOperationException("The library has moved from an Observer to a PubSub pattern. See the migration guides.");
    }

    /**
     * Retrieves the parent context of this session instance.
     *
     * @return the parent context of this Knowledge instance
     */
    Knowledge getParentContext();

    /**
     * <p>
     * Fires the rule session and returns the associated object that depends on the implementation of the current session.
     * </p>
     *
     * @return an object representing the result of the rule session execution.
     */
    Object fire();
}
