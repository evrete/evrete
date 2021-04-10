package org.evrete.runtime;

import org.evrete.api.ActiveField;

class KeyMemoryBucketNoAlpha extends KeyMemoryBucket {

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

    @Override
    final void flushBuffer() {
        if (current != DUMMY_FACT) {
            for (ActiveField field : activeFields) {
                fieldData.write(currentFactField(field));
            }
            fieldData.write(insertData);
            insertData.clear();
        }
    }
}
