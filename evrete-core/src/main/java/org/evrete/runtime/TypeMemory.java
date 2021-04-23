package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final MemoryActionBuffer buffer;
    private final FactStorage<FactRecord> factStorage;
    private final Type<?> type;
    private final ArrayOf<KeyMemory> betaMemories;
    private TypeMemoryState typeMemoryState;
    private int purgeActions = 0;

    TypeMemory(SessionMemory sessionMemory, int type) {
        super(sessionMemory);
        this.betaMemories = new ArrayOf<>(new KeyMemory[0]);
        Type<?> t = runtime.getTypeResolver().getType(type);
        this.type = t;
        this.buffer = new MemoryActionBuffer(configuration.getAsInteger(Configuration.INSERT_BUFFER_SIZE, Configuration.INSERT_BUFFER_SIZE_DEFAULT));
        String identityMethod = configuration.getProperty(Configuration.OBJECT_COMPARE_METHOD);
        switch (identityMethod) {
            case Configuration.IDENTITY_METHOD_EQUALS:
                this.factStorage = memoryFactory.newFactStorage(t, FactRecord.class, (o1, o2) -> Objects.equals(o1.instance, o2.instance));
                break;
            case Configuration.IDENTITY_METHOD_IDENTITY:
                this.factStorage = memoryFactory.newFactStorage(t, FactRecord.class, (o1, o2) -> o1.instance == o2.instance);
                break;
            default:
                throw new IllegalArgumentException("Invalid identity method '" + identityMethod + "' in the configuration. Expected values are '" + Configuration.IDENTITY_METHOD_EQUALS + "' or '" + Configuration.IDENTITY_METHOD_IDENTITY + "'");
        }


        updateCachedData();
    }

    void updateCachedData() {
        TypeResolver resolver = runtime.getTypeResolver();
        Type<?> t = resolver.getType(this.type.getId());
        TypeMemoryMetaData meta = runtime.getTypeMeta(t.getId());

        this.typeMemoryState = new TypeMemoryState(t, meta.activeFields, runtime.getEvaluators(), meta.alphaEvaluators);
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

    public FactStorage<FactRecord> getFactStorage() {
        return factStorage;
    }

    public ArrayOf<KeyMemory> getBetaMemories() {
        return betaMemories;
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

    private void forEachMemoryComponent(Consumer<KeyMemoryBucket> consumer) {
        betaMemories.forEach(fm -> fm.forEachBucket(consumer));
    }

    void commitBuffer() {
        betaMemories.forEach(KeyMemory::commitBuffer);

        if (purgeActions < 0) {
            // Performing data purge
            KeyMode scanMode = KeyMode.MAIN;
            Iterator<KeyMemory> it1 = betaMemories.iterator();
            while (it1.hasNext()) {
                KeyMemory keyMemory = it1.next();
                ReIterator<KeyMemoryBucket> buckets = keyMemory.getAlphaBuckets().iterator();
                while (buckets.hasNext()) {
                    KeyedFactStorage facts = buckets.next().getFieldData();
                    ReIterator<MemoryKey> keys = facts.keys(scanMode);
                    while (keys.hasNext()) {
                        MemoryKey key = keys.next();
                        ReIterator<FactHandleVersioned> handles = facts.values(scanMode, key);
                        while (handles.hasNext()) {
                            FactHandleVersioned handle = handles.next();
                            FactRecord fact = factStorage.getFact(handle.getHandle());
                            if (fact == null || fact.getVersion() != handle.getVersion()) {
                                // No such fact, deleting
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
        Collection<RuntimeFact> inserts = new LinkedList<>();

        while (it.hasNext()) {
            AtomicMemoryAction a = it.next();
            switch (a.action) {
                case RETRACT:
                    factStorage.delete(a.handle);
                    purgeActions++;
                    break;
                case INSERT:
                    inserts.add(new RuntimeFact(valueResolver, typeMemoryState, new FactHandleVersioned(a.handle), a.factRecord));
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
                        inserts.add(new RuntimeFact(valueResolver, typeMemoryState, versioned, factRecord));
                        purgeActions++;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        if (!inserts.isEmpty()) {
            // Performing insert
            forEachMemoryComponent(b -> b.insert(inserts));
        }
        buffer.clear();
    }

    public final KeyMemory get(FieldsKey fields) {
        KeyMemory fm = betaMemories.get(fields.getId());
        if (fm == null) {
            throw new IllegalArgumentException("No key memory exists for " + fields);
        } else {
            return fm;
        }
    }

    KeyMemoryBucket touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        return betaMemories
                .computeIfAbsent(
                        key.getId(),
                        i -> new KeyMemory(TypeMemory.this, key)
                )
                .getCreate(alphaMeta);
    }


    void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        KeyMemoryBucket bucket = touchMemory(key, meta);
        ReIterator<FactStorage.Entry<FactRecord>> allFacts = factStorage.iterator();
        List<RuntimeFact> runtimeFacts = new LinkedList<>();
        while (allFacts.hasNext()) {
            FactStorage.Entry<FactRecord> rec = allFacts.next();
            FactHandleVersioned fhv = new FactHandleVersioned(rec.getHandle(), rec.getInstance().getVersion());
            runtimeFacts.add(new RuntimeFact(valueResolver, typeMemoryState, fhv, rec.getInstance()));
        }

        bucket.insert(runtimeFacts);
        bucket.commitBuffer();
    }
}
