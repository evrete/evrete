package org.evrete.runtime.evaluation;

import org.evrete.runtime.FieldsKey;
import org.evrete.util.Bits;

public interface MemoryAddress {
    boolean isEmpty();

    int getBucketIndexOld();

    int getBucketIndex();

    boolean testAlphaBits(Bits mask);

    FieldsKey fields();
}
