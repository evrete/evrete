package org.evrete.api;

public interface MemoryKeyCollection extends ReIterable<MemoryKey> {

    void clear();

    void add(MemoryKey key);
}
