package org.evrete.runtime.evaluation;

import org.evrete.runtime.FieldsKey;
import org.evrete.util.Bits;

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

    boolean testAlphaBits(Bits mask);

    FieldsKey fields();
}
