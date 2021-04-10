package org.evrete.runtime;

class KeyMemoryBucketNoAlpha0 extends KeyMemoryBucketNoAlpha {

    KeyMemoryBucketNoAlpha0(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime, typeFields);
    }


    @Override
    final void flushBuffer() {
        if (current != DUMMY_FACT) {
            fieldData.write(insertData);
            insertData.clear();
        }
    }
}
