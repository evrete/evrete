package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.ValueHandle;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.Collection;
import java.util.LinkedList;

public abstract class KeyMemoryBucket extends MemoryComponent {
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

    KeyMemoryBucket(MemoryComponent runtime, MemoryAddress address) {
        super(runtime);
        FieldsKey fields = address.fields();
        this.fieldData = memoryFactory.newBetaStorage(fields.getFields().length);
        this.activeFields = fields.getFields();
    }

    static KeyMemoryBucket factory(MemoryComponent runtime, MemoryAddress address) {
        int fieldCount = address.fields().size();
        if (address.isEmpty()) {
            switch (fieldCount) {
                case 0:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlpha0(runtime, address);
                case 1:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlpha1(runtime, address);
                default:
                    return new KeyMemoryBucketNoAlpha.KeyMemoryBucketNoAlphaN(runtime, address);
            }
        } else {
            switch (fieldCount) {
                case 0:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlpha0(runtime, address);
                case 1:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlpha1(runtime, address);
                default:
                    return new KeyMemoryBucketAlpha.KeyMemoryBucketAlphaN(runtime, address);
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

    public final KeyedFactStorage getFieldData() {
        return fieldData;
    }

    void commitBuffer() {
        fieldData.commitChanges();
    }

    @Override
    public final String toString() {
        return fieldData.toString();
    }

    abstract static class KeyMemoryBucketAlpha extends KeyMemoryBucket {
        private final MemoryAddress memoryAddress;

        KeyMemoryBucketAlpha(MemoryComponent runtime, MemoryAddress address) {
            super(runtime, address);
            this.memoryAddress = address;
        }

        @Override
        final void insert(Iterable<RuntimeFact> facts) {
            current = DUMMY_FACT;
            for (RuntimeFact fact : facts) {
                if (memoryAddress.testAlphaBits(fact.alphaTests)) {
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

            KeyMemoryBucketAlpha0(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
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

            KeyMemoryBucketAlpha1(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
                assert address.fields().size() == 1;
                this.field = address.fields().getFields()[0];
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

            KeyMemoryBucketAlphaN(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
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

        KeyMemoryBucketNoAlpha(MemoryComponent runtime, MemoryAddress address) {
            super(runtime, address);
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

            KeyMemoryBucketNoAlpha0(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
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

            KeyMemoryBucketNoAlpha1(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
                assert address.fields().size() == 1;
                this.field = address.fields().getFields()[0];
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

            KeyMemoryBucketNoAlphaN(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
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
