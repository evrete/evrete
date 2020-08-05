package org.evrete.api;

public interface BufferedInsert {
    default void ensureExtraCapacity(int insertCount) {

    }
}
