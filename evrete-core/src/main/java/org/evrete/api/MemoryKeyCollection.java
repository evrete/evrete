package org.evrete.api;

/**
 * This interface represents a mutable extension to the {@link ReIterable} interface, specifically
 * for use with memory keys.
 */
public interface MemoryKeyCollection extends ReIterable<MemoryKey> {

    void clear();

    void add(MemoryKey key);
}
