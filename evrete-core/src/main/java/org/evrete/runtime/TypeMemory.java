package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class TypeMemory extends TypeMemoryBase {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private Cache cache;
    private final MemoryActionBuffer buffer;

    TypeMemory(SessionMemory sessionMemory, int type) {
        super(sessionMemory, type);
        int bufferSize = configuration.getAsInteger(Configuration.INSERT_BUFFER_SIZE, Configuration.INSERT_BUFFER_SIZE_DEFAULT);
        this.buffer = new MemoryActionBuffer(type, bufferSize, sessionMemory.runtime.deltaMemoryManager);
        updateCachedData();
    }

    void updateCachedData() {
        TypeResolver resolver = runtime.getTypeResolver();
        Type<?> t = resolver.getType(this.type.getId());
        TypeMemoryMetaData meta = runtime.getTypeMeta(t.getId());
        this.cache = new Cache(t, meta.activeFields, runtime.getEvaluators(), valueResolver, meta.alphaEvaluators);
    }

    private FactRecord getFactRecord(FactHandle handle) {
        FactRecord record = null;
        // Object may be in uncommitted state (updated), so we need check the action buffer first
        AtomicMemoryAction bufferedAction = buffer.get(handle);
        if (bufferedAction != null) {
            if (bufferedAction.action != Action.RETRACT) {
                record = bufferedAction.factRecord;
            }
        } else {
            record = getStoredRecord(handle);
        }
        return record;
    }

    public Object getFact(FactHandle handle) {
        FactRecord record = getFactRecord(handle);
        return record == null ? null : record.instance;
    }

    public MemoryActionBuffer getBuffer() {
        return buffer;
    }

    void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        factStorage.iterator().forEachRemaining(record -> {
            FactHandle handle = record.getHandle();
            Object fact = record.getInstance().instance;
            AtomicMemoryAction bufferedAction = buffer.get(handle);
            if (bufferedAction == null) {
                // No changes to this fact
                consumer.accept(handle, fact);
            } else {
                if (bufferedAction.action != Action.RETRACT) {
                    // Reporting changed data
                    consumer.accept(bufferedAction.handle, bufferedAction.factRecord.instance);
                }
            }
        });
    }

    FactHandle externalInsert(Object fact) {
        FactRecord record = new FactRecord(fact);
        FactHandle handle = factStorage.insert(record);
        if (handle == null) {
            LOGGER.warning("Fact " + fact + " has been already inserted");
            return null;
        } else {
            return add(Action.INSERT, handle, record);
        }
    }

    public FactHandle add(Action action, FactHandle factHandle, FactRecord factRecord) {
        buffer.add(action, factHandle, factRecord);
        return factHandle;
    }

    public RuntimeFact createFactRuntime(FactHandleVersioned factHandle, FactRecord factRecord) {
        return cache.createFactRuntime(factHandle, factRecord, valueResolver);
    }

    void onNewAlphaBucket(MemoryAddress address) {
        KeyMemoryBucket bucket = touchMemory(address);
        ReIterator<FactStorage.Entry<FactRecord>> allFacts = factStorage.iterator();
        List<RuntimeFact> runtimeFacts = new LinkedList<>();
        while (allFacts.hasNext()) {
            FactStorage.Entry<FactRecord> rec = allFacts.next();
            FactHandleVersioned fhv = new FactHandleVersioned(rec.getHandle(), rec.getInstance().getVersion());
            runtimeFacts.add(createFactRuntime(fhv, rec.getInstance()));
        }

        bucket.insert(runtimeFacts);
        bucket.commitBuffer();
    }

    /**
     * A state class to be used in memory initialization and hot deployment updates
     */
    static class Cache {
        final TypeField[] fields;
        final AlphaPredicate[] alphaEvaluators;
        final Object[] currentValues;
        final ValueResolver resolver;
        final boolean hasAlphaConditions;

        Cache(Type<?> type, ActiveField[] activeFields, Evaluators evaluators, ValueResolver resolver, AlphaEvaluator[] alphaEvaluators) {
            this.fields = new TypeField[activeFields.length];
            this.resolver = resolver;
            for (int i = 0; i < activeFields.length; i++) {
                this.fields[i] = type.getField(activeFields[i].field());
            }
            this.currentValues = new Object[this.fields.length];
            this.hasAlphaConditions = alphaEvaluators.length > 0;
            this.alphaEvaluators = new AlphaPredicate[alphaEvaluators.length];
            if (hasAlphaConditions) {
                for (int i = 0; i < alphaEvaluators.length; i++) {
                    this.alphaEvaluators[i] = new AlphaPredicate(alphaEvaluators[i], evaluators, currentValues);
                }
            }
        }

        private RuntimeFact createFactRuntime(FactHandleVersioned factHandle, FactRecord factRecord, ValueResolver valueResolver) {

            ValueHandle[] valueHandles = new ValueHandle[fields.length];
            BitSet alphaTests;

            if (hasAlphaConditions) {
                for (int i = 0; i < valueHandles.length; i++) {
                    TypeField f = fields[i];
                    Object fieldValue = f.readValue(factRecord.instance);
                    currentValues[i] = fieldValue;
                    valueHandles[i] = valueResolver.getValueHandle(f.getValueType(), fieldValue);
                }

                alphaTests = new BitSet();
                for (AlphaPredicate alphaEvaluator : alphaEvaluators) {
                    if (alphaEvaluator.test()) {
                        alphaTests.set(alphaEvaluator.getIndex());
                    }
                }

            } else {
                for (int i = 0; i < valueHandles.length; i++) {
                    TypeField f = fields[i];
                    valueHandles[i] = valueResolver.getValueHandle(f.getValueType(), f.readValue(factRecord.instance));
                }
                alphaTests = Mask.EMPTY;
            }

            return new RuntimeFact(factRecord, factHandle, valueHandles, alphaTests);
        }

    }

    static class AlphaPredicate {
        private final EvaluatorWrapper delegate;
        private final int index;
        private final IntToValue func;

        AlphaPredicate(AlphaEvaluator alphaEvaluator, Evaluators evaluators, Object[] values) {
            this.delegate = evaluators.get(alphaEvaluator.getDelegate());
            ActiveField[] activeDescriptor = alphaEvaluator.getDescriptor();
            this.index = alphaEvaluator.getIndex();

            int[] valueIndices = new int[activeDescriptor.length];
            for (int i = 0; i < valueIndices.length; i++) {
                valueIndices[i] = activeDescriptor[i].getValueIndex();
            }
            this.func = i -> values[valueIndices[i]];
        }

        int getIndex() {
            return index;
        }

        boolean test() {
            return delegate.test(func);
        }

    }
}
