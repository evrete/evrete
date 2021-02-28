package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    //private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;

    TypeMemory(SessionMemory sessionMemory, Type<?> type) {
        super(sessionMemory);
        this.type = type;
        //this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);

        String identityMethod = configuration.getOrDefault(Configuration.OBJECT_COMPARE_METHOD, Configuration.IDENTITY_METHOD_IDENTITY).toString();
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

    void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        MemoryComponent mc = touchMemory(key, meta);
        forEachEntry((fh, rec) -> mc.insert(new FactHandleVersioned(fh, rec.getVersion()), new LazyInsertState(valueResolver, rec)));
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
    final void onNewActiveField(ActiveField newField) {
        List<FactStorage.Entry<FactRecord>> data = new LinkedList<>();

        ReIterator<FactStorage.Entry<FactRecord>> allFacts = factStorage.iterator();
        while (allFacts.hasNext()) {
            data.add(allFacts.next());
        }

        for (FactStorage.Entry<FactRecord> rec : data) {
            Object fieldValue = newField.readValue(rec.getInstance().instance);
            ValueHandle valueHandle = valueResolver.getValueHandle(newField.getValueType(), fieldValue);
            rec.getInstance().appendValue(newField, valueHandle);
            factStorage.update(rec.getHandle(), rec.getInstance());
        }
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
