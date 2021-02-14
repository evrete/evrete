package org.evrete.api;


import java.util.Collection;

public interface WorkingMemory {

    FactHandle insert(Object fact);

    Object getFact(FactHandle handle);

    FactHandle insert(String type, Object fact);

    void update(FactHandle handle, Object newValue);

    void delete(FactHandle handle);

    default void insert(String factType, Collection<?> objects) {
        for (Object o : objects) {
            insert(factType, o);
        }
    }

    default void insert(Collection<?> objects) {
        for (Object o : objects) {
            insert(o);
        }
    }

    default void delete(Collection<FactHandle> factHandles) {
        for (FactHandle handle : factHandles) {
            delete(handle);
        }
    }

    default void insert(Object... objects) {
        for (Object o : objects) {
            insert(o);
        }
    }


}
