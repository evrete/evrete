package org.evrete.runtime.memory;

public interface BiMemoryComponent<T> {

    void addAll(T other);

    void clearData();
}
