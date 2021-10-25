package org.evrete.api;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.stream.Collector;

/**
 * <p>
 * Base interface for both stateful and stateless sessions
 * </p>
 */
public interface RuleSession<S extends RuleSession<S>> extends RuntimeContext<S>, RuleSet<RuntimeRule> {
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
     *
     * @return collector
     */
    <T> Collector<T, ?, S> asCollector();


    /**
     * @deprecated in favor of {@link #setExecutionPredicate(BooleanSupplier)}
     */
    @Deprecated
    default S setFireCriteria(BooleanSupplier criteria) {
        return setExecutionPredicate(criteria);
    }

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
     * @param type type name
     * @param fact fact to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     * @see #insertAs(String, Object)
     * @deprecated
     */
    @Deprecated
    default FactHandle insert(String type, Object fact) {
        return insertAs(type, fact);
    }

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
     * @see #insertAs(String, Object)
     * @deprecated
     */
    @Deprecated
    default S insert(String type, Collection<?> objects) {
        return insertAs(type, objects);
    }

    /**
     * <p>
     * Inserts a collection of facts, marking them as being of a specific type.
     * </p>
     *
     * @param type    type name
     * @param objects objects to insert
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
     */
    @SuppressWarnings("unchecked")
    default S insert(Object... objects) {
        insert0(objects, true);
        return (S) this;
    }

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    S addEventListener(SessionLifecycleListener listener);

    S removeEventListener(SessionLifecycleListener listener);

    Knowledge getParentContext();

    Object fire();
}
