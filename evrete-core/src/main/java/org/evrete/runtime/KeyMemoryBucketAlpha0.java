package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaBucketMeta;

class KeyMemoryBucketAlpha0 extends KeyMemoryBucketAlpha {

    KeyMemoryBucketAlpha0(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime, typeFields, alphaMask);
    }


    @Override
    final void flushBuffer() {
        if (current != DUMMY_FACT) {
            fieldData.write(insertData);
            insertData.clear();
        }
    }
}
