package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;

    TypeMemory(SessionMemory sessionMemory, Type<?> type) {
        super(sessionMemory);
        this.type = type;
        this.alphaBuckets = new ArrayOf<>(new TypeMemoryBucket[]{new TypeMemoryBucket(TypeMemory.this, AlphaBucketMeta.NO_FIELDS_NO_CONDITIONS)});

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

    FactHandle registerNewFact(FactRecord innerFact) {
        return this.factStorage.insert(innerFact);
    }

    public Type<?> getType() {
        return type;
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        alphaBuckets.forEach(consumer);
        betaMemories.values().forEach(consumer);
    }

    @Override
    protected void clearLocalData() {
        factStorage.clear();
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void processMemoryChange(Action action, FactHandle handle, FactRecord factRecord) {
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

    private void forEachSubComponent(Consumer<InnerFactMemory> consumer) {
        alphaBuckets.forEach(consumer);
        betaMemories.values().forEach(consumer);
    }


    @Override
    // TODO !!!! optimize by caching components as an array
    public void insert(FactHandleVersioned fact, FieldToValue key) {
        forEachSubComponent(im -> im.insert(fact, key));
    }

    @Override
    public void commitChanges() {
        for (TypeMemoryBucket bucket : this.alphaBuckets.data) {
            bucket.commitChanges();
        }

        for (FieldsMemory fm : this.betaMemories.values()) {
            fm.commitChanges();
        }
    }

    private void performUpdate(FactHandle handle, FactRecord factRecord) {
        // Reading the previous version
        FactRecord previous = factStorage.getFact(handle);
        if (previous == null) {
            LOGGER.warning("Unknown fact handle " + handle + ". Update operation skipped.");
        } else {
            int newVersion = previous.getVersion() + 1;
            factRecord.updateVersion(newVersion);
            factStorage.update(handle, factRecord);
            FactHandleVersioned versioned = new FactHandleVersioned(handle, newVersion);
            insert(versioned, factRecord);
        }
    }


    void forEachValidEntry(BiConsumer<FactHandle, Object> consumer) {
        forEachValidEntryInner(main0(), (v, rec) -> consumer.accept(v.getHandle(), rec.instance));
    }

    private void forEachValidEntryInner(SharedPlainFactStorage plainData, BiConsumer<FactHandleVersioned, FactRecord> consumer) {
        ReIterator<FactHandleVersioned> it = plainData.iterator();
        while (it.hasNext()) {
            FactHandleVersioned v = it.next();
            FactHandle handle = v.getHandle();
            FactRecord record = factStorage.getFact(handle);
            if (record != null && record.getVersion() == v.getVersion()) {
                consumer.accept(v, record);
            } else {
                it.remove();
            }
        }
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


    public PlainMemory get(AlphaBucketMeta alphaMask) {
        return alphaBuckets.getChecked(alphaMask.getBucketIndex());
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        if (key.size() == 0) {
            touchAlphaMemory(alphaMeta);
        } else {
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(TypeMemory.this, key))
                    .touchMemory(alphaMeta);
        }
    }

    private TypeMemoryBucket touchAlphaMemory(AlphaBucketMeta alphaMeta) {
        if (!alphaMeta.isEmpty()) {
            int bucketIndex = alphaMeta.getBucketIndex();
            if (alphaBuckets.isEmptyAt(bucketIndex)) {
                TypeMemoryBucket newBucket = new TypeMemoryBucket(TypeMemory.this, alphaMeta);
                alphaBuckets.set(bucketIndex, newBucket);
                return newBucket;
            }
        }
        return null;
    }

    void onNewAlphaBucket(AlphaDelta delta) {
        // 2. Create and fill buckets
        FieldsKey key = delta.getKey();
        AlphaBucketMeta meta = delta.getNewAlphaMeta();


        AlphaBucketMeta alphaMeta = delta.getNewAlphaMeta();
        if (key.size() == 0) {
            // 3. Create new alpha data bucket
            TypeMemoryBucket newBucket = touchAlphaMemory(alphaMeta);
            assert newBucket != null;
            // Fill data
            forEachValidEntryInner(main0(), (fhv, rec) -> {
                if (meta.test(rec)) {
                    newBucket.getData().insert(fhv);
                }
            });
        } else {

            // 3. Process keyed/beta-memory
            FieldsMemory m = getCreate(key);
            FieldsMemoryBucket bucket = m.touchMemory(meta);
            assert bucket != null;
            forEachValidEntryInner(main0(), (fhv, rec) -> {
                if (meta.test(rec)) {
                    //TODO !!! implement
                    throw new UnsupportedOperationException();
                    //bucket.getData().insert(fhv);
                }
            });
        }
    }

    private SharedPlainFactStorage main0() {
        return alphaBuckets.data[0].getData();
    }

    private SharedPlainFactStorage delta0() {
        return alphaBuckets.data[0].getDelta();
    }

    private FieldsMemory getCreate(FieldsKey key) {
        synchronized (this.betaMemories) {
            FieldsMemory m = this.betaMemories.get(key);
            if (m == null) {
                m = new FieldsMemory(this, key);
                this.betaMemories.put(key, m);
            }
            return m;
        }
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
        for (SharedPlainFactStorage storage : new SharedPlainFactStorage[]{main0(), delta0()}) {
            ReIterator<FactHandleVersioned> it = storage.iterator();
            while (it.hasNext()) {
                FactHandleVersioned rto = it.next();
                FactHandle handle = rto.getHandle();
                FactRecord rec = getFact(handle);
                if (rec != null) {
                    Object fieldValue = newField.readValue(rec.instance);
                    rec.appendValue(newField, fieldValue);
                    factStorage.update(handle, rec);
                }
            }

        }
    }
}
