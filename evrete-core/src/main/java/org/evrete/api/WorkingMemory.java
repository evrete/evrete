package org.evrete.api;

import java.util.Collection;

/**
 * <p>
 * An interface that every rule session implements and that describes operations on facts
 * that are available to developers.
 * </p>
 */
public interface WorkingMemory {

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
     * Returns fact by its handle.
     * </p>
     *
     * @param handle fact handle
     * @return fact or <code>null</code> if fact is not found
     */
    Object getFact(FactHandle handle);

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
     * Updates a fact that already exists in the working memory
     * </p>
     *
     * @param handle   fact handle, previously assigned to original fact
     * @param newValue an updated version of the fact
     */
    void update(FactHandle handle, Object newValue);

    /**
     * <p>
     * Deletes a fact from working memory.
     * </p>
     *
     * @param handle fact handle
     */
    void delete(FactHandle handle);

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
}
