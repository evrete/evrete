package org.evrete.api;

public interface SessionOps {
    /**
     * Inserts a fact in working memory and returns a serializable fact handle.
     * The engine will derive the fact's logical type from its Java class.
     *
     * @param fact object to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     * @see FactHandle
     * @see Type
     */
    default FactHandle insert(Object fact) {
        return insert0(fact, true);
    }

    /**
     * <p>
     * Inserts a fact in working memory and returns a serializable fact handle.
     * When {@code resolveCollections} is set to true, and the fact is an {@link Iterable}
     * or an Array, the engine will instead insert its components and return a null {@link FactHandle}.
     * The engine will derive logical type of the fact(s) from their Java class names.
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
     * @see Type
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
     * @param type               explicit logical type of the inserted fact(s)
     * @return fact handle assigned to the fact, or {@code null} if multiple facts were inserted
     * @throws NullPointerException if argument is null
     * @see FactHandle
     * @see Type
     */
    FactHandle insert0(String type, Object fact, boolean resolveCollections);

    /**
     * Deletes a fact from the working memory.
     *
     * @param handle The FactHandle associated with the fact to be deleted.
     */
    void delete(FactHandle handle);

    /**
     * Updates a fact that already exists in the working memory
     *
     * @param handle   fact handle, previously assigned to original fact
     * @param newValue an updated version of the fact
     */
    void update(FactHandle handle, Object newValue);

    /**
     * <p>
     * Inserts a fact and explicitly specifies its {@link Type} name.
     * </p>
     *
     * @param type explicit logical type of the inserted fact
     * @param fact fact to insert
     * @return fact handle assigned to the fact
     * @throws NullPointerException if argument is null
     * @see Type
     */
    default FactHandle insertAs(String type, Object fact) {
        return insert0(type, fact, false);
    }

}
