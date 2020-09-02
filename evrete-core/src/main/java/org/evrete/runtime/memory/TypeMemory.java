package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.collections.FastIdentityHashMap;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.RuntimeObject;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaConditions;
import org.evrete.runtime.evaluation.AlphaDelta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;

public final class TypeMemory implements PlainMemory {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final AlphaConditions alphaConditions;
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final SessionMemory runtime;
    private final Type type;
    private final IdentityMap mainFacts;
    private final IdentityMap deltaFacts;
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final List<RuntimeObject> insertBuffer = new LinkedList<>();
    private final List<RuntimeFact> deleteBuffer = new LinkedList<>();

    private ActiveField[] activeFields;

    private AlphaEvaluator[] alphaEvaluators;

    TypeMemory(SessionMemory runtime, Type type) {
        this.runtime = runtime;
        this.alphaConditions = runtime.getAlphaConditions();
        this.type = type;
        this.mainFacts = new IdentityMap();
        this.deltaFacts = new IdentityMap();
        this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void clear() {
        mainFacts.clear();
        deltaFacts.clear();
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
    public boolean hasChanges() {
        return deltaFacts.size() > 0;
    }

    @Override
    public void commitChanges() {
        if(deltaFacts.size() > 0) {
            mainFacts.bulkAdd(deltaFacts);
            deltaFacts.clear();
        }
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }

    }

    @Override
    public ReIterator<RuntimeFact> mainIterator() {
        return mainFacts.factIterator();
    }

    @Override
    public ReIterator<RuntimeFact> deltaIterator() {
        return deltaFacts.factIterator();
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
        for(IdentityMap map : new IdentityMap[]{mainFacts, deltaFacts}) {
            ReIterator<RuntimeObject> it = map.factImplIterator();
            while (it.hasNext()) {
                RuntimeObject rto = it.next();
                Object fieldValue = newField.readValue(rto.getDelegate());
                rto.appendValue(newField, fieldValue);
            }
        }


    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        if (key.size() == 0) {
            touchAlphaMemory(alphaMeta);
        } else {
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(runtime, key))
                    .touchMemory(alphaMeta);
        }
    }

    private TypeMemoryBucket touchAlphaMemory(AlphaBucketMeta alphaMeta) {
        // Alpha storage
        if (!alphaMeta.isEmpty()) {
            int bucketIndex = alphaMeta.getBucketIndex();
            if (alphaBuckets.isEmptyAt(bucketIndex)) {
                TypeMemoryBucket newBucket = new TypeMemoryBucket(runtime, alphaMeta);
                alphaBuckets.set(bucketIndex, newBucket);
                return newBucket;
            }
        }
        return null;
    }


    void onNewAlphaBucket(AlphaDelta delta) {

        //TODO !!!!!!
        if(deltaFacts.size() > 0 ) throw new IllegalStateException();

        ReIterator<RuntimeObject> existingFacts = mainFacts.factImplIterator();
        // 1. Update all the facts by applying new alpha flags
        AlphaEvaluator[] newEvaluators = delta.getNewEvaluators();
        if (newEvaluators.length > 0 && existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                existingFacts.next().appendAlphaTest(newEvaluators);
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
                    .computeIfAbsent(key, k -> new FieldsMemory(runtime, key))
                    .onNewAlphaBucket(alphaMeta, existingFacts);
        }
    }

    void commitMemoryDeltas() {
        // Commit main storage (no alpha-conditions)
        this.commitChanges();

        // Commit alpha-buckets
        for(TypeMemoryBucket b : alphaBuckets.data) {
            b.commitChanges();
        }

        // Commit beta-memories
        for(FieldsMemory fm : betaMemories.values()) {
            fm.commitChanges();
        }
    }

    final void doDelete() {
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
        RuntimeFact rtf = mainFacts.remove(fact);
        if (rtf != null) {
            deleteBuffer.add(rtf);
        }
    }

    void onBeforeChange() {
        this.alphaEvaluators = alphaConditions.getPredicates(type).data;
        this.activeFields = runtime.getActiveFields(type);
    }


    @SuppressWarnings("unchecked")
    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        mainFacts.forEachKey(f -> consumer.accept((T) f));
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        mainFacts.forEachKey(consumer);
    }

    final void insertSingle(Object o) {
        RuntimeObject rto = register(o);
        insertBuffer.add(rto);
    }

    private RuntimeObject register(Object o) {
        final RuntimeObject rto;

        // Read values
        Object[] values = new Object[activeFields.length];
        for (int i = 0; i < activeFields.length; i++) {
            values[i] = activeFields[i].readValue(o);
        }

        // Evaluate alpha conditions if necessary
        if (alphaEvaluators.length > 0) {
            boolean[] alphaTests = new boolean[alphaEvaluators.length];
            for (AlphaEvaluator alpha : alphaEvaluators) {
                int fieldInUseIndex = alpha.getValueIndex();
                alphaTests[alpha.getUniqueId()] = alpha.test(values[fieldInUseIndex]);
            }
            rto = RuntimeObject.factory(o, values, alphaTests);
        } else {
            rto = RuntimeObject.factory(o, values);
        }

        if(mainFacts.contains(o) || deltaFacts.contains(o)) {
            LOGGER.warning("Object " + o + " has been already inserted, skipping insert");
            return null;
        } else {
            deltaFacts.put(o, rto);
            return rto;
        }
    }

    private static class IdentityMap extends FastIdentityHashMap<Object, RuntimeObject> {
        private static final ToIntFunction<Object> HASH = System::identityHashCode;
        private static final Function<Entry<Object, RuntimeObject>, RuntimeFact> MAPPER = Entry::getValue;
        private static final Function<Entry<Object, RuntimeObject>, RuntimeObject> MAPPER_IMPL = Entry::getValue;

        private static final BiPredicate<Object, Object> EQ = (fact1, fact2) -> fact1 == fact2;

        ReIterator<RuntimeFact> factIterator() {
            return iterator(MAPPER);
        }

        ReIterator<RuntimeObject> factImplIterator() {
            return iterator(MAPPER_IMPL);
        }

        @Override
        protected ToIntFunction<Object> keyHashFunction() {
            return HASH;
        }

        @Override
        protected BiPredicate<Object, Object> keyHashEquals() {
            return EQ;
        }

        boolean contains(Object o) {
            return get(o) != null;
        }
    }
}
