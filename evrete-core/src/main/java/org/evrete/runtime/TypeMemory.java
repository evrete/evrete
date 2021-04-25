package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Bits;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class TypeMemory extends TypeMemoryBase {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private Cache cache;
    private int purgeActions = 0;
    private final MemoryActionBuffer buffer;

    TypeMemory(SessionMemory sessionMemory, int type) {
        super(sessionMemory, type);
        this.buffer = new MemoryActionBuffer(configuration.getAsInteger(Configuration.INSERT_BUFFER_SIZE, Configuration.INSERT_BUFFER_SIZE_DEFAULT));
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

    FactHandle externalInsert(Object fact, MemoryActionListener actionListener) {
        FactRecord record = new FactRecord(fact);
        FactHandle handle = factStorage.insert(record);
        if (handle == null) {
            LOGGER.warning("Fact " + fact + " has been already inserted");
            return null;
        } else {
            return add(Action.INSERT, handle, record, actionListener);
        }
    }

    private void purge(KeyMode... scanModes) {
        if (purgeActions > 0) {
            for (KeyMode scanMode : scanModes) {
                // Performing data purge
                Iterator<KeyMemoryBucket> buckets = memoryBuckets.iterator();
                while (buckets.hasNext()) {
                    KeyMemoryBucket bucket = buckets.next();
                    KeyedFactStorage facts = bucket.getFieldData();
                    ReIterator<MemoryKey> keys = facts.keys(scanMode);
                    while (keys.hasNext()) {
                        MemoryKey key = keys.next();
                        ReIterator<FactHandleVersioned> handles = facts.values(scanMode, key);
                        while (handles.hasNext()) {
                            FactHandleVersioned handle = handles.next();
                            FactRecord fact = factStorage.getFact(handle.getHandle());
                            if (fact == null || fact.getVersion() != handle.getVersion()) {
                                // No such fact, deleting
                                //System.out.println("Deleting " + handle + " from " + bucket);
                                handles.remove();
                            }
                        }

                        long remaining = handles.reset();
                        if (remaining == 0) {
                            // Deleting key as well
                            keys.remove();
                        }
                    }
                }
            }
            purgeActions = 0;
        }
    }

    public void processBuffer() {
        Iterator<AtomicMemoryAction> it = buffer.actions();
        Collection<RuntimeFact> inserts = new LinkedList<>();

        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            switch (a.action) {
                case RETRACT:
                    factStorage.delete(a.handle);
                    purgeActions++;
                    break;
                case INSERT:
                    inserts.add(createFactRuntime(new FactHandleVersioned(a.handle), a.factRecord));
                    break;
                case UPDATE:
                    FactRecord previous = factStorage.getFact(a.handle);
                    if (previous == null) {
                        LOGGER.warning("Unknown fact handle " + a.handle + ". Update operation skipped.");
                    } else {
                        FactRecord factRecord = a.factRecord;
                        FactHandle handle = a.handle;
                        int newVersion = previous.getVersion() + 1;
                        factRecord.updateVersion(newVersion);
                        factStorage.update(handle, factRecord);
                        FactHandleVersioned versioned = new FactHandleVersioned(handle, newVersion);
                        inserts.add(createFactRuntime(versioned, factRecord));
                        purgeActions++;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        if (!inserts.isEmpty()) {
            // Performing insert
            forEachBucket(b -> b.insert(inserts));
        }
        //purge(KeyMode.MAIN);
        buffer.clear();
    }

    public FactHandle add(Action action, FactHandle factHandle, FactRecord factRecord, MemoryActionListener listener) {
        buffer.add(action, factHandle, factRecord, listener);
        return factHandle;
    }

    private RuntimeFact createFactRuntime(FactHandleVersioned factHandle, FactRecord factRecord) {
        TypeField[] fields = cache.fields;
        ValueHandle[] valueHandles = new ValueHandle[fields.length];
        Bits alphaTests;

        if (cache.hasAlphaConditions) {
            for (int i = 0; i < valueHandles.length; i++) {
                TypeField f = fields[i];
                Object fieldValue = f.readValue(factRecord.instance);
                cache.currentValues[i] = fieldValue;
                valueHandles[i] = valueResolver.getValueHandle(f.getValueType(), fieldValue);
            }

            alphaTests = new Bits();
            for (AlphaPredicate alphaEvaluator : cache.alphaEvaluators) {
                if (alphaEvaluator.test()) {
                    alphaTests.set(alphaEvaluator.getIndex());
                }
            }

        } else {
            for (int i = 0; i < valueHandles.length; i++) {
                TypeField f = fields[i];
                valueHandles[i] = valueResolver.getValueHandle(f.getValueType(), f.readValue(factRecord.instance));
            }
            alphaTests = Bits.EMPTY;
        }

        return new RuntimeFact(factHandle, valueHandles, alphaTests);
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

        Cache(Type<?> type, ActiveField[] activeFields, Evaluators evaluators, ValueResolver resolver, org.evrete.runtime.evaluation.AlphaEvaluator[] alphaEvaluators) {
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
    }

    static class AlphaPredicate {
        private final EvaluatorWrapper delegate;
        private final ActiveField[] activeDescriptor;
        private final Object[] values;
        private final int index;

        AlphaPredicate(AlphaEvaluator alphaEvaluator, Evaluators evaluators, Object[] values) {
            this.delegate = evaluators.get(alphaEvaluator.getDelegate());
            this.activeDescriptor = alphaEvaluator.getDescriptor();
            this.index = alphaEvaluator.getIndex();
            this.values = values;
        }

        int getIndex() {
            return index;
        }

        public boolean test() {
            return delegate.test(i -> values[activeDescriptor[i].getValueIndex()]);
        }

    }
}
