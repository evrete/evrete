package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;
import org.evrete.util.NextIntSupplier;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends MemoryComponent {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    //private final AlphaConditions alphaConditions;
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    //private final ActionQueue inputBuffer = new ActionQueue();
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

    //TODO !!! rename
    FactHandle registerNewFact(FactRecord innerFact) {
        return this.factStorage.insert(innerFact);
    }

    public Type<?> getType() {
        return type;
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void clearLocalData() {
        factStorage.clear();
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void processMemoryChange(Action action, FactHandle handle, FactRecord factRecord, NextIntSupplier insertCounter) {
        switch (action) {
            case RETRACT:
                factStorage.delete(handle);
                return;
            case INSERT:
                performInsert(new FactHandleVersioned(handle), factRecord, insertCounter);
                return;
            case UPDATE:
                performUpdate(handle, factRecord, insertCounter);
                return;
            default:
                throw new IllegalStateException();
        }
    }

    private void forEachSubComponent(Consumer<InnerFactMemory> consumer) {
        alphaBuckets.forEach(consumer);
        betaMemories.values().forEach(consumer);
    }

    // TODO !!!! optimize
    private void performInsert(FactHandleVersioned handle, FactRecord factRecord, NextIntSupplier insertCounter) {
        forEachSubComponent(im -> {
            im.insert(handle, factRecord);
            insertCounter.next();
        });
    }

    private void performUpdate(FactHandle handle, FactRecord factRecord, NextIntSupplier insertCounter) {
        // Reading the previous version
        FactRecord previous = factStorage.getFact(handle);
        if (previous == null) {
            LOGGER.warning("Unknown fact handle " + handle + ". Update operation skipped.");
        } else {
            int newVersion = previous.getVersion() + 1;
            factRecord.updateVersion(newVersion);
            factStorage.update(handle, factRecord);
            FactHandleVersioned versioned = new FactHandleVersioned(handle, newVersion);
            performInsert(versioned, factRecord, insertCounter);
        }
    }


/*
    boolean bufferContains(Action... actions) {
        for (Action action : actions) {
            if (inputBuffer.hasData(action)) {
                return true;
            }
        }
        return false;
    }
*/

    void forEachValidEntry(BiConsumer<FactHandle, Object> consumer) {
        ReIterator<FactHandleVersioned> it = main0().iterator();
        while (it.hasNext()) {
            FactHandleVersioned v = it.next();
            FactHandle handle = v.getHandle();
            FactRecord record = factStorage.getFact(handle);
            if (record != null && record.getVersion() == v.getVersion()) {
                consumer.accept(handle, record.instance);
            } else {
                // TODO !!!! uncomment when the rest is tested
                //it.remove();
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

    void commitDeltas() {
        for (TypeMemoryBucket bucket : this.alphaBuckets.data) {
            bucket.commitChanges();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.commitDeltas();
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
/*
        if (inputBuffer.get(Action.INSERT).reset() > 0) {
            //TODO develop a strategy
            throw new UnsupportedOperationException("A new condition was created in an uncommitted memory.");
        }

        ReIterator<FactHandle> existingFacts = main0().iterator();
        // 1. Update all the facts by applying new alpha flags
        AlphaEvaluator[] newEvaluators = delta.getNewEvaluators();
        if (newEvaluators.length > 0 && existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                RuntimeFactImpl fact = (RuntimeFactImpl) existingFacts.next();

                fact.appendAlphaTest(newEvaluators);
            }
        }


        // 2. Create and fill buckets
        FieldsKey key = delta.getKey();
        AlphaBucketMeta alphaMeta = delta.getNewAlphaMeta();
        if (key.size() == 0) {
            // 3. Create new alpha data bucket
            TypeMemoryBucket newBucket = touchAlphaMemory(alphaMeta);
            assert newBucket != null;
            // Fill data
            newBucket.fillMainStorage(existingFacts);
        } else {
            // 3. Process keyed/beta-memory
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(getRuntime(), key))
                    .onNewAlphaBucket(alphaMeta, existingFacts);
        }

        this.cachedAlphaEvaluators = alphaConditions.getPredicates(type).data;
*/
    }

    @SuppressWarnings("unchecked")
    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        throw new UnsupportedOperationException();
/*
        main0().iterator().forEachRemaining(factHandle -> {
            Object fact = getFact(factHandle);
            if (fact != null) {
                consumer.accept((T) fact);
            }
        });
*/
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        throw new UnsupportedOperationException();
/*
        main0().iterator().forEachRemaining(factHandle -> {
            Object fact = getFact(factHandle);
            if (fact != null) {
                consumer.accept(fact);
            }
        });
*/
    }

    public FactStorage<FactRecord> getFactStorage() {
        return factStorage;
    }

    SharedPlainFactStorage main0() {
        return alphaBuckets.data[0].getData();
    }

    SharedPlainFactStorage delta0() {
        return alphaBuckets.data[0].getDelta();
    }

/*
    private RuntimeFactImpl create(FactHandleTuple tuple) {
        // Read values
        Object[] values = new Object[cachedActiveFields.length];
        for (int i = 0; i < cachedActiveFields.length; i++) {
            values[i] = cachedActiveFields[i].readValue(tuple.value);
        }

        // Evaluate alpha conditions if necessary
        if (cachedAlphaEvaluators.length > 0) {
            boolean[] alphaTests = new boolean[cachedAlphaEvaluators.length];
            for (AlphaEvaluator alpha : cachedAlphaEvaluators) {
                alphaTests[alpha.getUniqueId()] = alpha.test(values);
            }
            return RuntimeFactImpl.factory(tuple, values, alphaTests);
        } else {
            return RuntimeFactImpl.factory(tuple, values);
        }
    }
*/

    /**
     * <p>
     * Modifies existing facts by appending value of the newly
     * created field
     * </p>
     *
     * @param newField newly created field
     */
    final void onNewActiveField(ActiveField newField) {
        throw new UnsupportedOperationException();
/*
        for (SharedPlainFactStorage storage : new SharedPlainFactStorage[]{main0(), delta0()}) {
            ReIterator<RuntimeFact> it = storage.iterator();
            while (it.hasNext()) {
                RuntimeFactImpl rto = (RuntimeFactImpl) it.next();
                Object fieldValue = newField.readValue(rto.getDelegate());
                rto.appendValue(newField, fieldValue);
            }

        }
        this.cachedActiveFields = getRuntime().getActiveFields(type);
*/
    }

/*
    void memoryAction(Action action, FactHandle handle, Object o) {
        inputBuffer.add(action, handle, o);
    }
*/

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(1024);
        for (TypeMemoryBucket b : alphaBuckets.data) {


            s.append(b.getAlphaMask()).append("\n");
            s.append("\tM:").append(b.getData()).append('\n');
            s.append("\tD:").append(b.getDelta()).append('\n');

            for (FieldsMemory fm : this.betaMemories.values()) {
                s.append("\t\tFM:").append(fm).append('\n');

            }
        }

        return s.toString();
/*
        return "TypeMemory{" +
                "alphaBuckets=" + alphaBuckets +
                '}';
*/
    }

}
