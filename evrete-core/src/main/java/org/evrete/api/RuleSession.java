package org.evrete.api;


import java.util.Collection;

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
    default void insert(String type, Collection<?> objects) {
        if (objects != null) {
            for (Object o : objects) {
                insert(type, o);
            }
        }
    }

    /**
     * <p>
     * Inserts a collection of facts.
     * </p>
     *
     * @param objects objects to insert
     * @see #insert(Object)
     */
    default void insert(Collection<?> objects) {
        if (objects != null) {
            for (Object o : objects) {
                insert(o);
            }
        }
    }

    /**
     * <p>
     * Inserts an array of facts.
     * </p>
     *
     * @param objects objects to insert
     * @see #insert(Object)
     */
    default void insert(Object... objects) {
        if (objects != null) {
            for (Object o : objects) {
                insert(o);
            }
        }
    }

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    RuntimeContext<?> getParentContext();

}
