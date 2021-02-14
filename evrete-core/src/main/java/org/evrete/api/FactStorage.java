package org.evrete.api;

import java.util.Iterator;

public interface FactStorage<T> {

    /**
     * <p>
     * Creates and returns new FactHandle for each inserted fact. The contract is that the implementation must
     * return <code>null</code> if object is already known and not deleted.
     * </p>
     *
     * @param fact the fact being inserted in the working memory
     * @return null if object has been already inserted or a new FactHandle otherwise
     */
    FactHandle insert(T fact);

    //TODO !!!! rename to iterator(), in the implementation avoid extending hash collections
    Iterator<T> it();

    void delete(FactHandle handle);

    void update(FactHandle handle, T newInstance);

    T getFact(FactHandle handle);

    void clear();

}
