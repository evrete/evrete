package org.evrete.api;

/**
 * Represents a storage system for facts, allowing for operations such as insertion, deletion, updating, and retrieval of facts.
 * This storage system essentially acts as a mapping between fact handles (unique identifiers for facts) and the facts themselves.
 * The engine creates a {@code FactStorage} instance for each declared fact of a specific {@link Type}.
 *
 * @param <T> the type of facts that this storage system handles
 */
public interface FactStorage<T> extends ReIterable<FactStorage.Entry<T>> {

    /**
     * <p>
     * Creates and returns new FactHandle for each inserted fact. The contract is that the implementation must
     * return {@code null} if object is already known and not deleted.
     * </p>
     *
     * @param fact the fact being inserted in the working memory
     * @return null if object has been already inserted or a new FactHandle otherwise
     */
    FactHandle insert(T fact);

    /**
     * Deletes a fact from the storage, identified by the provided handle.
     *
     * @param handle the handle of the fact to be deleted
     */
    void delete(FactHandle handle);

    /**
     * Updates an existing fact in the storage with a new instance, identified by the provided handle.
     *
     * @param handle      the handle of the fact to be updated
     * @param newInstance the new instance to replace the existing fact
     */
    void update(FactHandle handle, T newInstance);

    /**
     * Retrieves the fact associated with the provided handle.
     *
     * @param handle the handle whose associated fact is to be retrieved
     * @return the fact associated with the provided handle
     */
    T getFact(FactHandle handle);

    /**
     * Clears all facts from the storage.
     */
    void clear();

    /**
     * Represents an entry in the storage, holding both a fact handle and the fact instance.
     *
     * @param <Z> the type of fact this entry holds
     */
    interface Entry<Z> {
        /**
         * Returns the FactHandle associated with this entry.
         *
         * @return the FactHandle associated with this entry
         */
        FactHandle getHandle();

        /**
         * Returns the fact instance associated with this entry.
         *
         * @return an instance of type Z.
         */
        Z getInstance();
    }
}
