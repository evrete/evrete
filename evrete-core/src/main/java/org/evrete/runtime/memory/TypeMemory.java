package org.evrete.runtime.memory;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.collections.FastIdentityHashMap;
import org.evrete.runtime.MemoryChangeListener;
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

public final class TypeMemory implements MemoryChangeListener, ReIterable<RuntimeFact> {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final AlphaConditions alphaConditions;
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final SessionMemory runtime;
    private final Type type;
    private final IdentityMap facts;
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final List<RuntimeObject> insertBuffer = new LinkedList<>();
    private final List<RuntimeFact> deleteBuffer = new LinkedList<>();

    private ActiveField[] activeFields;

    private AlphaEvaluator[] alphaEvaluators;

    TypeMemory(SessionMemory runtime, Type type) {
        this.runtime = runtime;
        this.alphaConditions = runtime.getAlphaConditions();
        Configuration conf = runtime.getConfiguration();
        this.type = type;
        this.facts = new IdentityMap(conf);
        this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void clear() {
        facts.clear();
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

    public void forEach(Consumer<RuntimeFact> consumer) {
        facts.forEachValue(consumer::accept);
    }

    @Override
    public ReIterator<RuntimeFact> iterator() {
        return this.facts.factIterator();
    }

    public ReIterable<RuntimeFact> get(AlphaBucketMeta alphaMask) {
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
        ReIterator<RuntimeObject> it = facts.factImplIterator();
        while (it.hasNext()) {
            RuntimeObject rto = it.next();
            Object fieldValue = newField.readValue(rto.getDelegate());
            rto.appendValue(newField, fieldValue);
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
        ReIterator<RuntimeObject> existingFacts = facts.factImplIterator();
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
            if (existingFacts.reset() > 0) {
                while (existingFacts.hasNext()) {
                    newBucket.insertSingle(existingFacts.next());
                }
            }
        } else {
            // 3. Process keyed/beta-memory
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(runtime, key))
                    .onNewAlphaBucket(alphaMeta, existingFacts);
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
        RuntimeFact rtf = facts.remove(fact);
        if (rtf != null) {
            deleteBuffer.add(rtf);
        }
    }

    @Override
    public void onBeforeChange() {
        this.alphaEvaluators = alphaConditions.getPredicates(type).data;
        this.activeFields = runtime.getActiveFields(type);
        for (FieldsMemory fm : fieldsMemories()) {
            fm.onBeforeChange();
        }
    }

    @Override
    public void onAfterChange() {
        for (FieldsMemory fm : fieldsMemories()) {
            fm.onAfterChange();
        }
    }

    @SuppressWarnings("unchecked")
    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        facts.forEachKey(f -> consumer.accept((T) f));
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        facts.forEachKey(consumer);
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

        if (facts.put(o, rto) == null) {
            return rto;
        } else {
            LOGGER.warning("Object " + o + " has been already inserted, skipping insert");
            return null;
        }
    }

    private static class IdentityMap extends FastIdentityHashMap<Object, RuntimeObject> {
        private static final ToIntFunction<Object> HASH = System::identityHashCode;
        private static final Function<Entry<Object, RuntimeObject>, RuntimeFact> MAPPER = Entry::getValue;
        private static final Function<Entry<Object, RuntimeObject>, RuntimeObject> MAPPER_IMPL = Entry::getValue;

        private static final BiPredicate<Object, Object> EQ = (fact1, fact2) -> fact1 == fact2;

        IdentityMap(Configuration conf) {
            super(conf.getExpectedObjectCount());
        }

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
    }
}
