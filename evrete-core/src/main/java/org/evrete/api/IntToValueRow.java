package org.evrete.api;

@FunctionalInterface
public interface IntToValueRow {

    MemoryKey apply(int value);
}
