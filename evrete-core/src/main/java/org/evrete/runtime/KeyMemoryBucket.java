package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;

import static org.evrete.runtime.RuntimeFact.DUMMY_FACT;

public abstract class KeyMemoryBucket extends MemoryComponent {
    final KeyedFactStorage fieldData;
    final ActiveField[] activeFields;
    final Collection<FactHandleVersioned> buffer = new LinkedList<>();
    RuntimeFact current = null;
    final MemoryAddress address;

    KeyMemoryBucket(MemoryComponent runtime, MemoryAddress address) {
        super(runtime);
        FieldsKey fields = address.fields();
        this.fieldData = memoryFactory.newBetaStorage(fields.getFields().length);
        this.activeFields = fields.getFields();
        this.address = address;
    }

    boolean purgeDeleted(Predicate<FactHandleVersioned> predicate) {
        ReIterator<MemoryKey> keys = fieldData.keys(KeyMode.OLD_OLD);
        boolean ret = false;
        while (keys.hasNext()) {
            MemoryKey key = keys.next();
            ReIterator<FactHandleVersioned> handles = fieldData.values(KeyMode.OLD_OLD, key);
            while (handles.hasNext()) {
                FactHandleVersioned handle = handles.next();
                if (predicate.test(handle)) {
                    handles.remove();
                }
            }
            long remaining = handles.reset();
            if (remaining == 0) {
                // Deleting key as well
                keys.remove();
                ret = true;
            }
        }
        return ret;
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

    /**
     * @param facts
     * @return true if at least one fact passed alpha tests and got saved
     */
    abstract boolean insert(Iterable<RuntimeFact> facts);

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

        KeyMemoryBucketAlpha(MemoryComponent runtime, MemoryAddress address) {
            super(runtime, address);
        }

        @Override
        final boolean insert(Iterable<RuntimeFact> facts) {
            current = DUMMY_FACT;
            boolean ret = false;
            for (RuntimeFact fact : facts) {
                if (address.testAlphaBits(fact.alphaTests)) {
                    ret = true;
                    fact.factRecord.markLocation(address);
                    if (current.sameValues(fact)) {
                        buffer.add(fact.factHandle);
                    } else {
                        // Key changed, ready for batch insert
                        flushBuffer();
                        buffer.add(fact.factHandle);
                        current = fact;
                    }
                }
            }

            if (!buffer.isEmpty()) {
                flushBuffer();
            }
            return ret;
        }

        static class KeyMemoryBucketAlpha0 extends KeyMemoryBucketAlpha {

            KeyMemoryBucketAlpha0(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
            }

            @Override
            final void flushBuffer() {
                if (current != DUMMY_FACT) {
                    fieldData.write(buffer);
                    buffer.clear();
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
                    fieldData.write(buffer);
                    buffer.clear();
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
                    fieldData.write(buffer);
                    buffer.clear();
                }
            }
        }
    }

    abstract static class KeyMemoryBucketNoAlpha extends KeyMemoryBucket {

        KeyMemoryBucketNoAlpha(MemoryComponent runtime, MemoryAddress address) {
            super(runtime, address);
        }

        @Override
        final boolean insert(Iterable<RuntimeFact> facts) {
            current = DUMMY_FACT;
            boolean ret = false;
            for (RuntimeFact fact : facts) {
                ret = true;
                fact.factRecord.markLocation(address);
                if (current.sameValues(fact)) {
                    buffer.add(fact.factHandle);
                } else {
                    // Key changed, ready for batch insert
                    flushBuffer();
                    buffer.add(fact.factHandle);
                    current = fact;
                }
            }
            if (!buffer.isEmpty()) {
                flushBuffer();
            }
            return ret;
        }

        static class KeyMemoryBucketNoAlpha0 extends KeyMemoryBucketNoAlpha {

            KeyMemoryBucketNoAlpha0(MemoryComponent runtime, MemoryAddress address) {
                super(runtime, address);
            }

            @Override
            final void flushBuffer() {
                if (current != DUMMY_FACT) {
                    fieldData.write(buffer);
                    buffer.clear();
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
                    fieldData.write(buffer);
                    buffer.clear();
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
                    fieldData.write(buffer);
                    buffer.clear();
                }
            }
        }
    }
}
