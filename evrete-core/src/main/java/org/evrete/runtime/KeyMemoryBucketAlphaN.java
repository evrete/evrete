package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

class KeyMemoryBucketAlphaN extends KeyMemoryBucketAlpha {

    KeyMemoryBucketAlphaN(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime, typeFields, alphaMask);
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
