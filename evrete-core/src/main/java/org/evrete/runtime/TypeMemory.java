package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.util.Bits;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    final MemoryActionBuffer buffer;
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;
    ActiveField[] activeFields;
    AlphaEvaluator[] alphaEvaluators;
    //TODO !!!! performance, switch to ArrayOf
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();


    TypeMemory(SessionMemory sessionMemory, Type<?> type, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators) {
        super(sessionMemory);
        this.type = type;
        this.activeFields = activeFields;
        this.alphaEvaluators = alphaEvaluators;
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
        //rebuildCachedData();
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
                record = bufferedAction.factRecord.record;
            }
        } else {
            record = getStoredRecord(handle);
        }
        return record;
    }

    FactRecord getStoredRecord(FactHandle handle) {
        return factStorage.getFact(handle);
    }

    public void add(Action action, FactHandle factHandle, LazyInsertState factRecord, MemoryActionListener listener) {
        buffer.add(action, factHandle, factRecord, listener);
    }

    /*

    FactRecord getFact(FactHandle handle) {
        return factStorage.getFact(handle);
    }
*/


    LazyInsertState buildFactRecord(Object instance) {
        ValueHandle[] valueHandles = new ValueHandle[activeFields.length];
        // We will need field values for lazy alpha tests thus avoiding
        // extra calls to ValueResolver
        Object[] transientFieldValues = new Object[activeFields.length];
        FactRecord record = new FactRecord(instance, valueHandles);

        for (ActiveField field : activeFields) {
            int idx = field.getValueIndex();
            Object fieldValue = field.readValue(instance);
            valueHandles[idx] = valueResolver.getValueHandle(field.getValueType(), fieldValue);
            transientFieldValues[idx] = fieldValue;
        }

        Bits alphaTests = new Bits();
        FieldToValue fieldToValues = new FieldToValue() {
            @Override
            public Object apply(ActiveField activeField) {
                return transientFieldValues[activeField.getValueIndex()];
            }
        };
        for (AlphaEvaluator evaluator : alphaEvaluators) {
            if (evaluator.test(fieldToValues)) {
                alphaTests.set(evaluator.getIndex());
            }
        }


        return new LazyInsertState(record, alphaTests, transientFieldValues);
    }


    FactHandle registerNewFact(LazyInsertState insertState) {
        return this.factStorage.insert(insertState.record);
    }

    public Type<?> getType() {
        return type;
    }

    @Override
    protected void clearLocalData() {
        factStorage.clear();
    }

    void processMemoryChange(Action action, FactHandle handle, LazyInsertState factRecord) {
        switch (action) {
            case RETRACT:
                factStorage.delete(handle);
                return;
            case INSERT:
                insert(new FactHandleVersioned(handle), factRecord);
                return;
            case UPDATE:
                performUpdate(handle, factRecord);
                return;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    void insert(FactHandleVersioned value, LazyInsertState insertState) {
        for (MemoryComponent child : childComponents()) {
            child.insert(value, insertState);
        }
    }

    @Override
    public void commitChanges() {
        for (MemoryComponent child : childComponents()) {
            child.commitChanges();
        }
    }

    private void performUpdate(FactHandle handle, LazyInsertState state) {
        // Reading the previous version
        FactRecord factRecord = state.record;
        FactRecord previous = factStorage.getFact(handle);
        if (previous == null) {
            LOGGER.warning("Unknown fact handle " + handle + ". Update operation skipped.");
        } else {
            int newVersion = previous.getVersion() + 1;
            factRecord.updateVersion(newVersion);
            factStorage.update(handle, factRecord);
            FactHandleVersioned versioned = new FactHandleVersioned(handle, newVersion);
            insert(versioned, state);
        }
    }

    // TODO !!! two similar forEach, analyze usage
    void forEachEntry(BiConsumer<FactHandle, FactRecord> consumer) {
        factStorage
                .iterator()
                .forEachRemaining(entry -> consumer.accept(entry.getHandle(), entry.getInstance()));
    }

    // TODO !!! two similar forEach, analyze usage
    void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        factStorage.iterator().forEachRemaining(new Consumer<FactStorage.Entry<FactRecord>>() {
            @Override
            public void accept(FactStorage.Entry<FactRecord> record) {
                FactHandle handle = record.getHandle();
                Object fact = record.getInstance().instance;
                AtomicMemoryAction bufferedAction = buffer.get(handle);
                if (bufferedAction == null) {
                    // No changes to this fact
                    consumer.accept(handle, fact);
                } else {
                    if (bufferedAction.action != Action.RETRACT) {
                        // Reporting changed data
                        consumer.accept(bufferedAction.handle, bufferedAction.factRecord.record.instance);
                    }
                }
            }
        });
    }

    void processBuffer() {
        Iterator<AtomicMemoryAction> it = buffer.actions();
        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            processMemoryChange(a.action, a.handle, a.factRecord);
        }
        buffer.clear();
    }

    public final FieldsMemory get(FieldsKey fields) {
        FieldsMemory fm = betaMemories.get(fields);
        if (fm == null) {
            throw new IllegalArgumentException("No key memory exists for " + fields);
        } else {
            return fm;
        }
    }

    MemoryComponent touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        return betaMemories
                .computeIfAbsent(key, k -> new FieldsMemory(TypeMemory.this, key))
                .getCreate(alphaMeta);
    }


    void onNewAlphaBucket(FieldsKey key, AlphaEvaluator[] newTypeAlphaEvaluators, AlphaBucketMeta meta) {
        MemoryComponent mc = touchMemory(key, meta);
        this.alphaEvaluators = newTypeAlphaEvaluators;
        forEachEntry(new BiConsumer<FactHandle, FactRecord>() {
            @Override
            public void accept(FactHandle fh, FactRecord rec) {
                FactHandleVersioned fhv = new FactHandleVersioned(fh, rec.getVersion());
                Object[] fieldValues = new Object[rec.getFieldValues().length];
                ValueHandle[] valueHandles = rec.getFieldValues();
                for (int i = 0; i < valueHandles.length; i++) {
                    fieldValues[i] = valueResolver.getValue(valueHandles[i]);
                }


                Bits alphaTests = new Bits();
                FieldToValue fieldToValues = new FieldToValue() {
                    @Override
                    public Object apply(ActiveField activeField) {
                        return fieldValues[activeField.getValueIndex()];
                    }
                };
                for (AlphaEvaluator evaluator : newTypeAlphaEvaluators) {
                    if (evaluator.test(fieldToValues)) {
                        alphaTests.set(evaluator.getIndex());
                    }
                }

                LazyInsertState state = new LazyInsertState(rec, alphaTests, fieldValues);
                mc.insert(fhv, state);

            }
        });
        mc.commitChanges();
    }

    /**
     * <p>
     * Modifies existing facts by appending value of the newly
     * created field
     * </p>
     *
     * @param newField newly created field
     */
    final void onNewActiveField(ActiveField newField, ActiveField[] newFields) {
        ReIterator<FactStorage.Entry<FactRecord>> allFacts = factStorage.iterator();
        while (allFacts.hasNext()) {
            FactStorage.Entry<FactRecord> rec = allFacts.next();
            Object fieldValue = newField.readValue(rec.getInstance().instance);
            ValueHandle valueHandle = valueResolver.getValueHandle(newField.getValueType(), fieldValue);
            rec.getInstance().appendValue(newField, valueHandle);
            factStorage.update(rec.getHandle(), rec.getInstance());
        }
        this.activeFields = newFields;
    }

    @Override
    public String toString() {
        String facts = "\n" + factStorage.toString();
        facts = facts.replaceAll("\n", "\n\t\t");

        StringJoiner betaJ = new StringJoiner("\n");
        betaMemories.forEach((fieldsKey, fieldsMemory) -> {
            String s = "\n" + fieldsKey + "\n\t" + fieldsMemory;
            betaJ.add(s);
        });

        String beta = betaJ.toString();
        beta = beta.replaceAll("\n\t\n", "\n");
        beta = beta.replaceAll("\n", "\n\t\t");


        return "type=" + type.getJavaType().getSimpleName() +
                "\n\tbeta=" + beta +
                "\n\tfacts=" + facts;
    }
}
