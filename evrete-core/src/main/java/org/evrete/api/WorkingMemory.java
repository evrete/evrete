package org.evrete.api;


import java.util.Collection;
import java.util.function.Consumer;

public interface WorkingMemory {

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

    default void delete(Collection<?> objects) {
        for (Object o : objects) {
            delete(o);
        }
    }

    default void update(Collection<?> objects) {
        for (Object o : objects) {
            update(o);
        }
    }

    void clear();

    void insert(Object fact);

    void update(Object fact);

    void delete(Object fact);

    void insert(String factType, Object fact);

    <T> void forEachMemoryObject(String type, Consumer<T> consumer);

    default <T> void forEachMemoryObject(Class<T> type, Consumer<T> consumer) {
        forEachMemoryObject(type.getName(), consumer);
    }

    void forEachMemoryObject(Consumer<Object> consumer);

    default void insert(Object... objects) {
        for (Object o : objects) {
            insert(o);
        }
    }

    default void insertTyped(String factType, Object... objects) {
        for (Object o : objects) {
            insert(factType, o);
        }
    }

    default void delete(Object... objects) {
        for (Object o : objects) {
            delete(o);
        }
    }

    default void update(Object... objects) {
        for (Object o : objects) {
            update(o);
        }
    }
}
