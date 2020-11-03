package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.RuntimeFactImpl;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaConditions;
import org.evrete.runtime.evaluation.AlphaDelta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends TypeMemoryBase {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final AlphaConditions alphaConditions;
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final Type<?> type;
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final List<RuntimeFact> insertBuffer = new LinkedList<>();
    private final List<RuntimeFact> deleteBuffer = new LinkedList<>();

    private ActiveField[] cachedActiveFields;
    private AlphaEvaluator[] cachedAlphaEvaluators;

    TypeMemory(SessionMemory runtime, Type<?> type) {
        super(runtime);
        this.alphaConditions = runtime.getAlphaConditions();
        this.type = type;
        this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);
        this.cachedActiveFields = runtime.getActiveFields(type);
        this.cachedAlphaEvaluators = alphaConditions.getPredicates(type).data;
    }


    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void processInput(Action action, Queue<Object> iterator) {
        Object o;

        switch (action) {
            case RETRACT:
                while ((o = iterator.poll()) != null) {
                    deleteSingle(o);
                }
                commitDelete();
                break;
            case INSERT:
                while ((o = iterator.poll()) != null) {
                    insertSingle(o);
                }
                commitInsert();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    void clear() {
        super.clearData();
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.clear();
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


    @Override
    public void commitChanges() {
        super.mergeDelta1();
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }
    }

    public PlainMemory get(AlphaBucketMeta alphaMask) {
        if (alphaMask.isEmpty()) {
            return this;
        }
        int bucketIndex = alphaMask.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalStateException("No alpha memory initialized for " + alphaMask + ", type: " + type);
        } else {
            TypeMemoryBucket bucket = alphaBuckets.data[bucketIndex];
            if (bucket == null) {
                throw new IllegalStateException("No alpha memory initialized for " + alphaMask + ", type: " + type);
            } else {
                return bucket;
            }
        }
    }

    final void commitInsert() {
        if (insertBuffer.isEmpty()) return;
        //Save to non-beta memory
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.insert(insertBuffer);
        }
        //Save to beta memory
        for (FieldsMemory fm : fieldsMemories()) {
            fm.insert(insertBuffer);
        }
        this.insertBuffer.clear();
    }

    /**
     * <p>
     * Modifies existing facts by appending value of the newly
     * created field
     * </p>
     *
     * @param newField newly created field
     */
    void onNewActiveField(ActiveField newField) {
        for (MemoryScope scope : MemoryScope.values()) {
            TypeMemoryComponent component = get(scope);
            ReIterator<RuntimeFact> it = component.iterator();
            while (it.hasNext()) {
                RuntimeFactImpl rto = (RuntimeFactImpl) it.next();
                Object fieldValue = newField.readValue(rto.getDelegate());
                rto.appendValue(newField, fieldValue);
            }

        }
        this.cachedActiveFields = getRuntime().getActiveFields(type);
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        if (key.size() == 0) {
            touchAlphaMemory(alphaMeta);
        } else {
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(getRuntime(), key))
                    .touchMemory(alphaMeta);
        }
    }

    private TypeMemoryBucket touchAlphaMemory(AlphaBucketMeta alphaMeta) {
        // Alpha storage
        if (!alphaMeta.isEmpty()) {
            int bucketIndex = alphaMeta.getBucketIndex();
            if (alphaBuckets.isEmptyAt(bucketIndex)) {
                TypeMemoryBucket newBucket = new TypeMemoryBucket(getRuntime(), alphaMeta);
                alphaBuckets.set(bucketIndex, newBucket);
                return newBucket;
            }
        }
        return null;
    }

    void onNewAlphaBucket(AlphaDelta delta) {

        if (get(MemoryScope.DELTA).totalFacts() > 0) {
            //TODO develop a strategy
            throw new UnsupportedOperationException("A new condition was created in an uncommitted memory.");
        }

        ReIterator<RuntimeFact> existingFacts = get(MemoryScope.MAIN).iterator();
        // 1. Update all the facts by applying new alpha flags
        AlphaEvaluator[] newEvaluators = delta.getNewEvaluators();
        if (newEvaluators.length > 0 && existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                ((RuntimeFactImpl) existingFacts.next()).appendAlphaTest(newEvaluators);
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
    }

    void commitMemoryDeltas() {
        // Commit main storage (no alpha-conditions)
        this.commitChanges();

        // Commit alpha-buckets
        for (TypeMemoryBucket b : alphaBuckets.data) {
            b.commitChanges();
        }

        // Commit beta-memories
        for (FieldsMemory fm : betaMemories.values()) {
            fm.commitChanges();
        }
    }

    final void commitDelete() {
        if (deleteBuffer.isEmpty()) return;
        //Delete from non-beta memory
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.retract(deleteBuffer);
        }
        //Delete from beta memory
        for (FieldsMemory fm : fieldsMemories()) {
            fm.retract(deleteBuffer);
        }
        this.deleteBuffer.clear();
    }

    private Collection<FieldsMemory> fieldsMemories() {
        return betaMemories.values();
    }

    void deleteSingle(Object fact) {
        RuntimeFact rtf = get(MemoryScope.MAIN).remove(fact);
        if (rtf != null) {
            deleteBuffer.add(rtf);
        }
    }


    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        get(MemoryScope.MAIN).forEachMemoryObject(consumer);
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        get(MemoryScope.MAIN).forEachObjectUnchecked(consumer);
    }

    final void insertSingle(Object o) {
        RuntimeFactImpl rto = mapToHandle(o);
        if (rto != null) {
            insertBuffer.add(rto);
        }
    }

    private RuntimeFactImpl mapToHandle(Object o) {
        //TODO !!! delete two conditions below
        if (get(MemoryScope.MAIN).contains(o)) {
            LOGGER.warning("!!!! Object " + o + " has been already inserted, skipping insert");
            return null;
        }
        if (get(MemoryScope.DELTA).contains(o)) {
            LOGGER.warning("????? Object " + o + " has been already inserted, skipping insert");
            return null;
        }


        if (get(MemoryScope.MAIN).contains(o) || get(MemoryScope.DELTA).contains(o)) {
            LOGGER.warning("Object " + o + " has been already inserted, skipping insert");
            return null;
        } else {
            final RuntimeFactImpl rto;

            // Read values
            Object[] values = new Object[cachedActiveFields.length];
            for (int i = 0; i < cachedActiveFields.length; i++) {
                values[i] = cachedActiveFields[i].readValue(o);
            }

            // Evaluate alpha conditions if necessary
            if (cachedAlphaEvaluators.length > 0) {
                boolean[] alphaTests = new boolean[cachedAlphaEvaluators.length];
                for (AlphaEvaluator alpha : cachedAlphaEvaluators) {
                    int fieldInUseIndex = alpha.getValueIndex();
                    alphaTests[alpha.getUniqueId()] = alpha.test(values[fieldInUseIndex]);
                }
                rto = RuntimeFactImpl.factory(o, values, alphaTests);
            } else {
                rto = RuntimeFactImpl.factory(o, values);
            }


            get(MemoryScope.DELTA).put(o, rto);
            return rto;
        }
    }
}
