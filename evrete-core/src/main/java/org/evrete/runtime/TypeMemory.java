package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.util.Bits;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final MemoryActionBuffer buffer;
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;
    private final ArrayOf<FieldsMemory> betaMemories;
    private final ReusableFieldValues reusableFieldValues;
    private TypeMemoryState typeMemoryState;

    TypeMemory(SessionMemory sessionMemory, TypeMemoryState initialState) {
        super(sessionMemory);
        this.betaMemories = new ArrayOf<>(new FieldsMemory[0]);
        this.type = initialState.type;
        this.buffer = new MemoryActionBuffer(configuration.getAsInteger(Configuration.INSERT_BUFFER_SIZE, Configuration.INSERT_BUFFER_SIZE_DEFAULT));
        String identityMethod = configuration.getProperty(Configuration.OBJECT_COMPARE_METHOD);
        switch (identityMethod) {
            case Configuration.IDENTITY_METHOD_EQUALS:
                this.factStorage = memoryFactory.newFactStorage(type, FactRecord.class, (o1, o2) -> Objects.equals(o1.instance, o2.instance));
                break;
            case Configuration.IDENTITY_METHOD_IDENTITY:
                this.factStorage = memoryFactory.newFactStorage(type, FactRecord.class, (o1, o2) -> o1.instance == o2.instance);
                break;
            default:
                throw new IllegalArgumentException("Invalid identity method '" + identityMethod + "' in the configuration. Expected values are '" + Configuration.IDENTITY_METHOD_EQUALS + "' or '" + Configuration.IDENTITY_METHOD_IDENTITY + "'");
        }
        this.reusableFieldValues = new ReusableFieldValues(valueResolver);
        updateCachedData(initialState);
    }

    void updateCachedData(TypeMemoryState state) {
        this.typeMemoryState = state;
        this.reusableFieldValues.updateStructure(state.activeFields, state.alphaEvaluators);
    }

    public Object getFact(FactHandle handle) {
        FactRecord record = getFactRecord(handle);
        return record == null ? null : record.instance;
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

    FactRecord getStoredRecord(FactHandle handle) {
        return factStorage.getFact(handle);
    }

    public FactHandle add(Action action, FactHandle factHandle, FactRecord factRecord, MemoryActionListener listener) {
        buffer.add(action, factHandle, factRecord, listener);
        return factHandle;
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

    public Type<?> getType() {
        return type;
    }

    @Override
    protected void clearLocalData() {
        factStorage.clear();
    }


/*
    private void performInsert(FactRecord factRecord, FactHandleVersioned handle) {
        reusableFieldValues.update(factRecord);
        insert(reusableFieldValues, reusableFieldValues.alphaTests(), handle);
    }
*/

    @Override
    void insert(LazyValues values, Bits alphaTests, FactHandleVersioned value) {
        for (MemoryComponent child : childComponents()) {
            child.insert(values, alphaTests, value);
        }
    }

    private void forEachMemoryComponent(Consumer<FieldsMemoryBucket> consumer) {
        betaMemories.forEach(fm -> fm.forEachBucket(consumer));
    }


    @Override
    public void commitChanges() {
        for (MemoryComponent child : childComponents()) {
            child.commitChanges();
        }
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

    public void processBuffer() {
        Iterator<AtomicMemoryAction> it = buffer.actions();
        Collection<RuntimeFact> runtimeFacts = new LinkedList<>();
        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            switch (a.action) {
                case RETRACT:
                    factStorage.delete(a.handle);
                    break;
                case INSERT:
                    runtimeFacts.add(new RuntimeFact(typeMemoryState, new FactHandleVersioned(a.handle), a.factRecord));
                    //performInsert(a.factRecord, new FactHandleVersioned(a.handle));
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
                        //RuntimeFact runtimeFact = new RuntimeFact(typeMemoryState, versioned, factRecord);
                        runtimeFacts.add(new RuntimeFact(typeMemoryState, versioned, factRecord));
                        //performInsert(factRecord, versioned);
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        // Performing insert
        if (!runtimeFacts.isEmpty()) {
            forEachMemoryComponent(b -> b.insert(runtimeFacts));
        }
        buffer.clear();
    }

    public final FieldsMemory get(FieldsKey fields) {
        FieldsMemory fm = betaMemories.get(fields.getId());
        if (fm == null) {
            throw new IllegalArgumentException("No key memory exists for " + fields);
        } else {
            return fm;
        }
    }

    FieldsMemoryBucket touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        return betaMemories
                .computeIfAbsent(
                        key.getId(),
                        i -> new FieldsMemory(TypeMemory.this, key)
                )
                .getCreate(alphaMeta);
    }


    void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        FieldsMemoryBucket bucket = touchMemory(key, meta);
        ReIterator<FactStorage.Entry<FactRecord>> allFacts = factStorage.iterator();
        List<RuntimeFact> runtimeFacts = new LinkedList<>();
        while (allFacts.hasNext()) {
            FactStorage.Entry<FactRecord> rec = allFacts.next();
            FactHandleVersioned fhv = new FactHandleVersioned(rec.getHandle(), rec.getInstance().getVersion());
            runtimeFacts.add(new RuntimeFact(typeMemoryState, fhv, rec.getInstance()));
        }

        bucket.insert(runtimeFacts);
        bucket.commitChanges();
    }

    private static class ReusableFieldValues implements LazyValues {
        private static final Bits EMPTY_BITS = new Bits();
        private final ValueResolver valueResolver;
        private ActiveField[] activeFields;
        private AlphaEvaluator[] alphaEvaluators;
        private ValueHandle[] valueHandles;
        private Object[] fieldValues;
        private FieldToValue alphaFunction;
        private Bits alphaTests;
        private FieldToValueHandle lazyValues;
        private FieldToValueHandle lazyValuesStatic;
        private int keyHash;

        ReusableFieldValues(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
        }

        void updateStructure(ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators) {
            this.activeFields = activeFields;
            this.alphaEvaluators = alphaEvaluators;
            this.fieldValues = new Object[activeFields.length];
            this.valueHandles = new ValueHandle[activeFields.length];
            this.alphaFunction = field -> fieldValues[field.getValueIndex()];
            this.lazyValuesStatic = field -> valueHandles[field.getValueIndex()];
        }

        @Override
        public FieldToValueHandle getValues() {
            if (lazyValues == null) {
                int idx;
                this.keyHash = 0;
                Object fieldValue;
                for (ActiveField field : activeFields) {
                    idx = field.getValueIndex();
                    fieldValue = fieldValues[idx];
                    ValueHandle h = valueResolver.getValueHandle(field.getValueType(), fieldValue);
                    valueHandles[idx] = h;
                    this.keyHash += 37 * h.hashCode();
                }

                lazyValues = lazyValuesStatic;
            }
            return lazyValues;
        }

        @Override
        public int keyHash() {
            return this.keyHash;
        }

        void update(FactRecord factRecord) {
            this.lazyValues = null;
            Object instance = factRecord.instance;
            for (ActiveField field : activeFields) {
                fieldValues[field.getValueIndex()] = field.readValue(instance);
            }

            if (alphaEvaluators.length == 0) {
                this.alphaTests = EMPTY_BITS;
            } else {
                this.alphaTests = new Bits();
                for (AlphaEvaluator evaluator : alphaEvaluators) {
                    if (evaluator.test(alphaFunction)) {
                        this.alphaTests.set(evaluator.getIndex());
                    }
                }
            }
        }

        Bits alphaTests() {
            return alphaTests;
        }
    }
}
