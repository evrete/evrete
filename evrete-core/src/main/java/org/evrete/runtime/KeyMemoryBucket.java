package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.ValueHandle;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;
import java.util.LinkedList;

abstract class KeyMemoryBucket extends MemoryComponent {
    // A convenience fact instance that is never equal to others
    private static final RuntimeFact DUMMY_FACT = new RuntimeFact() {
        @Override
        boolean sameValues(RuntimeFact other) {
            return false;
        }
    };

    final KeyedFactStorage fieldData;
    final ActiveField[] activeFields;
    final Collection<FactHandleVersioned> insertData = new LinkedList<>();
    RuntimeFact current = null;

    KeyMemoryBucket(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime);
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields().length);
        this.activeFields = typeFields.getFields();
    }

    static KeyMemoryBucket factory(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        if (alphaMask.isEmpty()) {
            switch (typeFields.size()) {
                case 0:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlpha0(runtime, typeFields);
                case 1:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlpha1(runtime, typeFields);
                default:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlphaN(runtime, typeFields);
            }
        } else {
            switch (typeFields.size()) {
                case 0:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlpha0(runtime, typeFields, alphaMask);
                case 1:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlpha1(runtime, typeFields, alphaMask);
                default:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlphaN(runtime, typeFields, alphaMask);
            }
        }
    }

    ValueHandle currentFactField(ActiveField field) {
        return current.getValue(field);
    }

    abstract void flushBuffer();

    abstract void insert(Iterable<RuntimeFact> facts);

    @Override
    protected final void clearLocalData() {
        fieldData.clear();
    }

    final KeyedFactStorage getFieldData() {
        return fieldData;
    }

    @Override
    final public void commitChanges() {
        fieldData.commitChanges();
    }

    @Override
    public final String toString() {
        return fieldData.toString();
    }

    abstract static class KeyMemoryBucketAlpha extends KeyMemoryBucket {
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

        static class KeyMemoryBucketAlpha0 extends KeyMemoryBucketAlpha {

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

        static class KeyMemoryBucketAlpha1 extends KeyMemoryBucketAlpha {
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

        static class KeyMemoryBucketAlphaN extends KeyMemoryBucketAlpha {

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
    }

    abstract static class KeyMemoryBucketNoAlpha extends KeyMemoryBucket {

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

        static class KeyMemoryBucketNoAlpha0 extends KeyMemoryBucketNoAlpha {

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

        static class KeyMemoryBucketNoAlpha1 extends KeyMemoryBucketNoAlpha {
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

        static class KeyMemoryBucketNoAlphaN extends KeyMemoryBucketNoAlpha {

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
    }
}
