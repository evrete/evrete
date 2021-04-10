package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

class KeyMemoryBucketAlpha1 extends KeyMemoryBucketAlpha {
    private final ActiveField field;

    KeyMemoryBucketAlpha1(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime, typeFields, alphaMask);
        assert typeFields.size() == 1;
        this.field = typeFields.getFields()[0];
    }

    @Override
    final void flushBuffer() {
        if (current != DUMMY_FACT) {
            fieldData.write(currentFactField(field));
            fieldData.write(insertData);
            insertData.clear();
        }
    }
}
