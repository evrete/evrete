package org.evrete.api;

@FunctionalInterface
public interface IntToMemoryKey {

    MemoryKey apply(int value);
}
