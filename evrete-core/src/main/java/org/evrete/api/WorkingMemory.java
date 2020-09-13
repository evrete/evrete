package org.evrete.api;


import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public interface WorkingMemory {

    void insert(String factType, Collection<?> objects);

    void insert(Collection<?> objects);

    void delete(Collection<?> objects);

    void update(Collection<?> objects);

    //TODO !!! clear condition nodes' data as well
    void clear();

    <T> void forEachMemoryObject(String type, Consumer<T> consumer);

    void forEachMemoryObject(Consumer<Object> consumer);

    default void insert(Object... objects) {
        insert(Arrays.asList(objects));
    }

    default void insertTyped(String factType, Object... objects) {
        insert(factType, Arrays.asList(objects));
    }

    default void delete(Object... objects) {
        delete(Arrays.asList(objects));
    }

    default void update(Object... objects) {
        update(Arrays.asList(objects));
    }
}
