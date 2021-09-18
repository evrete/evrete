package org.evrete.api;


import java.util.Collection;
import java.util.function.BooleanSupplier;

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
    FactHandle insert(Object fact);

    /**
     * Method renamed, use the {@link #setExecutionPredicate(BooleanSupplier)} instead.
     */
    @Deprecated
    default S setFireCriteria(BooleanSupplier criteria) {
        return setExecutionPredicate(criteria);
    }

    /**
     * <p>
     * Session call's the supplier's {@link BooleanSupplier#getAsBoolean()} method prior to each
     * activation cycle. If the provided value is <code>false</code> then the cycle gets interrupted
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
     * Inserts a fact and explicitly specifies its {@link Type} name.
     * </p>
     *
     * @param type type name
     * @param fact fact to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     */
    FactHandle insert(String type, Object fact);

    /**
     * <p>
     * Inserts a collection of facts, marking them as being of a specific type.
     * </p>
     *
     * @param type    type name
     * @param objects objects to insert
     * @see #insert(String, Object)
     */
    @SuppressWarnings("unchecked")
    default S insert(String type, Collection<?> objects) {
        if (objects != null) {
            for (Object o : objects) {
                insert(type, o);
            }
        }
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
    default S insert(Collection<?> objects) {
        if (objects != null) {
            for (Object o : objects) {
                insert(o);
            }
        }
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
        if (objects != null) {
            for (Object o : objects) {
                insert(o);
            }
        }
        return (S) this;
    }

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    S addEventListener(SessionLifecycleListener listener);

    S removeEventListener(SessionLifecycleListener listener);

    Knowledge getParentContext();

}
