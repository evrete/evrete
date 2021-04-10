package org.evrete.runtime;

import org.evrete.api.ActiveField;

class KeyMemoryBucketNoAlpha1 extends KeyMemoryBucketNoAlpha {
    private final ActiveField field;

    KeyMemoryBucketNoAlpha1(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime, typeFields);
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
