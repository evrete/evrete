package org.evrete.api;

import java.util.function.BooleanSupplier;
import java.util.stream.Collector;

/**
 * <p>
 * Base interface for both stateful and stateless sessions
 * </p>
 */
public interface RuleSession<S extends RuleSession<S>> extends RuleSetContext<S, RuntimeRule> {
    /**
     * <p>
     * Inserts a fact in working memory and returns a serializable fact handle.
     * </p>
     *
     * @param fact object to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     * @see FactHandle
     */
    default FactHandle insert(Object fact) {
        return insert0(fact, true);
    }


    /**
     * <p>
     * A convenience method that returns an instance of {@link Collector} for inserting
     * streams of facts.
     * </p>
     * @param <T> the type of input elements to the reduction operation
     * @return collector
     */
    <T> Collector<T, ?, S> asCollector();


    /**
     * <p>
     * Session call's the supplier's {@link BooleanSupplier#getAsBoolean()} method prior to each
     * activation cycle. If the provided value is {@code false} then the cycle gets interrupted
     * and session exits its fire(...) method.
     * </p>
     * <p>
     * Along with the {@link ActivationManager}, this method can be used to debug rules, to avoid infinite
     * activation loops, or to prevent excessive consumption of computer resources.
     * </p>
     *
     * @param criteria - boolean value supplier
     * @return this session
     */
    S setExecutionPredicate(BooleanSupplier criteria);


    /**
     * <p>
     * Inserts a fact in working memory and returns a serializable fact handle.
     * When {@code resolveCollections} is set to true, and the fact is an {@link Iterable}
     * or an Array, the engine will instead insert its components and return a null {@link FactHandle}.
     * </p>
     * <p>
     * Together with the {@link #insert0(String, Object, boolean)} method, this operation constitutes
     * the core insert operations that are actually implemented by the engine. The other insert
     * methods are just convenience wrappers of the two.
     * </p>
     *
     * @param fact               object to insert
     * @param resolveCollections collection/array inspection flag
     * @return fact handle assigned to the fact, or {@code null} if multiple facts were inserted
     * @throws NullPointerException if argument is null
     * @see FactHandle
     */
    FactHandle insert0(Object fact, boolean resolveCollections);

    /**
     * <p>
     * Inserts a fact and explicitly specifies its {@link Type} name.
     * When {@code resolveCollections} is set to true, and the fact is an {@link Iterable}
     * or an Array, the engine will instead insert its components, and return a null {@link FactHandle}.
     * </p>
     * <p>
     * Together with the {@link #insert0(Object, boolean)} method, this operation constitutes
     * the core insert operations that are actually implemented by the engine. The other insert
     * methods are just convenience wrappers of the two.
     * </p>
     *
     * @param fact               object to insert
     * @param resolveCollections collection/array inspection flag
     * @param type               type name
     * @return fact handle assigned to the fact, or {@code null} if multiple facts were inserted
     * @throws NullPointerException if argument is null
     * @see FactHandle
     */
    FactHandle insert0(String type, Object fact, boolean resolveCollections);

    /**
     * <p>
     * Inserts a fact and explicitly specifies its {@link Type} name.
     * </p>
     *
     * @param type type name
     * @param fact fact to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     */
    default FactHandle insertAs(String type, Object fact) {
        return insert0(type, fact, false);
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
     * @see #insert(Object)
     * @return this instance
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
     */
    S addEventListener(SessionLifecycleListener listener);

    /**
     * Removes a {@link SessionLifecycleListener} from the session.
     *
     * @param listener the {@link SessionLifecycleListener} to be removed
     * @return the current instance of the session
     */
    S removeEventListener(SessionLifecycleListener listener);

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
