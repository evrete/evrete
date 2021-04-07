package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;
    private ActiveField[] activeFields;
    private AlphaEvaluator[] alphaEvaluators;

    TypeMemory(SessionMemory sessionMemory, Type<?> type, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators) {
        super(sessionMemory);
        this.type = type;
        this.activeFields = activeFields;
        this.alphaEvaluators = alphaEvaluators;
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

    void forEachEntry(BiConsumer<FactHandle, FactRecord> consumer) {
        factStorage
                .iterator()
                .forEachRemaining(entry -> consumer.accept(entry.getHandle(), entry.getInstance()));
    }

    public final FieldsMemory get(FieldsKey fields) {
        FieldsMemory fm = betaMemories.get(fields);
        if (fm == null) {
            throw new IllegalArgumentException("No key memory exists for " + fields);
        } else {
            return fm;
        }
    }

    FactRecord getFact(FactHandle handle) {
        return factStorage.getFact(handle);
    }

    MemoryComponent touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        return betaMemories
                .computeIfAbsent(key, k -> new FieldsMemory(TypeMemory.this, key))
                .getCreate(alphaMeta);
    }


/*
    private LazyInsertState buildFactRecord(Type<?> type, Object instance) {
        ValueResolver valueResolver = memory.valueResolver;
        ActiveField[] activeFields = getActiveFields(type);
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
        return new LazyInsertState(record, transientFieldValues);
    }
*/


    void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        MemoryComponent mc = touchMemory(key, meta);
        forEachEntry(new BiConsumer<FactHandle, FactRecord>() {
            @Override
            public void accept(FactHandle fh, FactRecord rec) {
                FactHandleVersioned fhv = new FactHandleVersioned(fh, rec.getVersion());
                Object[] fieldValues = new Object[rec.getFieldValues().length];
                ValueHandle[] valueHandles = rec.getFieldValues();
                for (int i = 0; i < valueHandles.length; i++) {
                    fieldValues[i] = valueResolver.getValue(valueHandles[i]);
                }


                LazyInsertState state = new LazyInsertState(rec, fieldValues);
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
