package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaBucketMeta;

abstract class KeyMemoryBucketAlpha extends KeyMemoryBucket {
    private final AlphaBucketMeta alphaMask;

    KeyMemoryBucketAlpha(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime, typeFields);
        this.alphaMask = alphaMask;
    }

    @Override
    final void insert(Iterable<RuntimeFact> facts) {
        current = DUMMY_FACT;
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact.alphaTests)) {
                if (current.sameValues(fact)) {
                    insertData.add(fact.factHandle);
                } else {
                    // Key changed, ready for batch insert
                    flushBuffer();
                    insertData.add(fact.factHandle);
                    current = fact;
                }
            }
        }

        if (!insertData.isEmpty()) {
            flushBuffer();
        }
    }
}
