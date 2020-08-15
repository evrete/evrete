package org.evrete.runtime.memory;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.AbstractFastHashMap;
import org.evrete.collections.ArrayOf;
import org.evrete.collections.FastIdentityHashMap;
import org.evrete.runtime.*;

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

/*
    public boolean hasFieldsMemory(FieldsKey fields) {
        return betaMemories.containsKey(fields);
    }

    public final FieldsMemory getCreate(FieldsKey fields) {
        return betaMemories
                .computeIfAbsent(fields, k -> new FieldsMemory(runtime, fields));
    }
*/

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

    public ReIterable<RuntimeFact> get(AlphaMask alphaMask) {
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

    /**
     * If not exists, this method initializes memory structures and
     * fills them with existing facts (if any)
     *
     * @param key  beta/alpha key
     * @param mask alpha mask
     */
    public void init(FieldsKey key, AlphaMask mask) {
        assert this.type == key.getType();
        ReIterator<AbstractFastHashMap.Entry<Object, RuntimeObject>> factIterator = facts.iterator();
        if (key.size() == 0) {
            // Plain alpha storage
            TypeMemoryBucket bucket = init(mask);
            if (bucket != null) {
                // This alpha mask is new and requires data fill
                ArrayOf<AlphaEvaluator> alphas = alphaConditions.getPredicates(type);
                AlphaEvaluator newEvaluator = alphas.data[mask.getBucketIndex()];
                if (factIterator.reset() > 0) {
                    // Existing memory is not empty
                    if (mask.isEmpty()) {

                    } else {

                    }
                    while (factIterator.hasNext()) {
                        RuntimeObject rto = factIterator.next().getValue();
                        bucket.insertSingle(rto.appendAlphaTest(newEvaluator));
                    }
                }
            }
        } else {
            // Beta memory
            FieldsMemory fm = betaMemories.get(key);
            if (fm == null) {
                fm = new FieldsMemory(runtime, key);
                betaMemories.put(key, fm);
            }
            FieldsMemoryBucket betaBucket = fm.init(mask);
            if (betaBucket != null) {
                if (factIterator.reset() > 0) {
                    throw new UnsupportedOperationException();
                }
            }

        }
    }


    public TypeMemoryBucket init(AlphaMask alphaMask) {
        if (alphaMask.isEmpty()) {
            return null;
        } else {
            int bucketIndex = alphaMask.getBucketIndex();
            TypeMemoryBucket bucket;
            if (bucketIndex >= this.alphaBuckets.data.length) {
                bucket = new TypeMemoryBucket(runtime, alphaMask);
                this.alphaBuckets.append(bucket);
                return bucket;
            } else {
                bucket = this.alphaBuckets.data[bucketIndex];
                if (bucket == null) {
                    throw new IllegalStateException();
                } else {
                    return null;
                }
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

    Collection<FieldsMemory> fieldsMemories() {
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

        private static final BiPredicate<Object, Object> EQ = (fact1, fact2) -> fact1 == fact2;

        IdentityMap(Configuration conf) {
            super(conf.getExpectedObjectCount());
        }


        ReIterator<RuntimeFact> factIterator() {
            return iterator(MAPPER);
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
