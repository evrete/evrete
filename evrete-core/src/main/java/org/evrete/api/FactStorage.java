package org.evrete.api;

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

    void delete(FactHandle handle);

    void update(FactHandle handle, T newInstance);

    T getFact(FactHandle handle);

    void clear();

    interface Entry<Z> {
        FactHandle getHandle();

        Z getInstance();
    }
}
