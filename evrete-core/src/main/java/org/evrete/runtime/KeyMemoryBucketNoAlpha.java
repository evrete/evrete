package org.evrete.runtime;

abstract class KeyMemoryBucketNoAlpha extends KeyMemoryBucket {

    KeyMemoryBucketNoAlpha(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime, typeFields);
    }

    @Override
    final void insert(Iterable<RuntimeFact> facts) {
        current = DUMMY_FACT;
        for (RuntimeFact fact : facts) {
            if (current.sameValues(fact)) {
                insertData.add(fact.factHandle);
            } else {
                // Key changed, ready for batch insert
                flushBuffer();
                insertData.add(fact.factHandle);
                current = fact;
            }
        }
        if (!insertData.isEmpty()) {
            flushBuffer();
        }
    }
}
