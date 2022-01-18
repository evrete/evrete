package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class TypeMemory extends TypeMemoryBase {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private Cache cache;

    TypeMemory(SessionMemory sessionMemory, int type) {
        super(sessionMemory, type);
        updateCachedData();
    }

    void updateCachedData() {
        this.cache = new Cache(this.type, getRuntime());
    }

    private FactRecord getFactRecord(FactHandle handle) {
        return getStoredRecord(handle);
    }

    public Object getFact(FactHandle handle) {
        FactRecord record = getFactRecord(handle);
        return record == null ? null : record.instance;
    }


    void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        factStorage.iterator().forEachRemaining(record -> {
            FactHandle handle = record.getHandle();
            Object fact = record.getInstance().instance;
            consumer.accept(handle, fact);
        });
    }

    Optional<FactTuple> register(Object fact) {
        FactRecord record = new FactRecord(fact);
        FactHandle handle = factStorage.insert(record);
        if (handle == null) {
            LOGGER.warning("Fact " + fact + " has been already inserted");
            return Optional.empty();
        } else {
            return Optional.of(new FactTuple(handle, record));
        }
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
        final boolean hasAlphaConditions;

        Cache(Type<?> type, AbstractRuleSession<?> runtime) {
            Type<?> t = runtime.getType(type.getId());
            TypeMemoryMetaData meta = runtime.getTypeMeta(t.getId());

            this.fields = new TypeField[meta.activeFields.length];
            for (int i = 0; i < meta.activeFields.length; i++) {
                this.fields[i] = type.getField(meta.activeFields[i].field());
            }
            this.currentValues = new Object[this.fields.length];
            this.hasAlphaConditions = meta.alphaEvaluators.length > 0;
            this.alphaEvaluators = new AlphaPredicate[meta.alphaEvaluators.length];
            if (hasAlphaConditions) {
                for (int i = 0; i < alphaEvaluators.length; i++) {
                    this.alphaEvaluators[i] = new AlphaPredicate(meta.alphaEvaluators[i], runtime.getEvaluators(), currentValues);
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
            this.delegate = evaluators.get(alphaEvaluator.getDelegate(), false);
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
