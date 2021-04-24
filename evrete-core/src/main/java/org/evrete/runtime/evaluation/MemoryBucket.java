package org.evrete.runtime.evaluation;

import org.evrete.util.Bits;

public interface MemoryBucket {
    int getBucketIndex();

    boolean test(Bits mask);
}
