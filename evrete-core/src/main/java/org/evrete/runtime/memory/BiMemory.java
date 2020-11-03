package org.evrete.runtime.memory;

public interface BiMemory<Z, T extends BiMemoryComponent<Z>> {

    T get(MemoryScope scope);

    default void mergeDelta1() {
        throw new UnsupportedOperationException();
    }
}
