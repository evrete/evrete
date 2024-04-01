package org.evrete.runtime;

import java.util.BitSet;

public interface MemoryAddress {
    boolean isEmpty();

    /**
     * @return global and unique index across all fact types
     */
    int getId();

    /**
     * @return bucket index inside type memory
     */
    int getBucketIndex();

    boolean testAlphaBits(BitSet mask);

    FieldsKey fields();
}
