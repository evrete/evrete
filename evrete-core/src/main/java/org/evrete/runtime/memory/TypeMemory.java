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
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    //private final List<RuntimeFact> insertBuffer = new LinkedList<>();

    private final ActionQueue<RuntimeFact> inputBuffer = new ActionQueue<>();

    TypeMemory(SessionMemory runtime, Type<?> type) {
        super(runtime, type);
        this.alphaConditions = runtime.getAlphaConditions();
        this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);
    }


    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }


    public RuntimeFact doAction(Action action, Object o) {
        RuntimeFact fact;
        switch (action) {
            case INSERT:
            case UPDATE:
                fact = mapToHandle(o);
                break;
            case RETRACT:
                fact = deleteObject(o);
                break;
            default:
                throw new IllegalStateException();

        }
        inputBuffer.add(action, fact);
        return fact;
    }


    RuntimeFact deleteObject(Object o) {
        RuntimeFact fact = get(MemoryScope.MAIN).remove(o);
        if (fact == null) {
            fact = get(MemoryScope.DELTA).remove(o);
        }
        return fact;
    }

    public void processInputBuffer(Action action) {
        Collection<RuntimeFact> facts = inputBuffer.get(action);
        if (facts.isEmpty()) return;
        switch (action) {
            case INSERT:
                for (TypeMemoryBucket bucket : alphaBuckets.data) {
                    bucket.insert(facts);
                }
                for (FieldsMemory fm : fieldsMemories()) {
                    fm.insert(facts);
                }
                break;
            case RETRACT:
                for (TypeMemoryBucket bucket : alphaBuckets.data) {
                    bucket.retract(facts);
                }
                for (FieldsMemory fm : fieldsMemories()) {
                    fm.retract(facts);
                }
                break;
            default:
                throw new IllegalStateException("Unsupported action " + action);

        }
        facts.clear();
    }

    void clear() {
        super.clearData();
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.clear();
        }
        inputBuffer.clear();
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

/*
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
*/


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

    private Collection<FieldsMemory> fieldsMemories() {
        return betaMemories.values();
    }

    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        get(MemoryScope.MAIN).forEachMemoryObject(consumer);
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        get(MemoryScope.MAIN).forEachObjectUnchecked(consumer);
    }

/*
    final void insertSingle(Object o) {
        RuntimeFactImpl rto = mapToHandle(o);
        if (rto != null) {
            insertBuffer.add(rto);
        }
    }
*/

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
