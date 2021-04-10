package org.evrete.runtime;

import org.evrete.api.ActiveField;

class KeyMemoryBucketNoAlphaN extends KeyMemoryBucketNoAlpha {

    KeyMemoryBucketNoAlphaN(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime, typeFields);
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
