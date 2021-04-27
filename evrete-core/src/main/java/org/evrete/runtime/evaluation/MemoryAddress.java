package org.evrete.runtime.evaluation;

import org.evrete.runtime.FieldsKey;

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
