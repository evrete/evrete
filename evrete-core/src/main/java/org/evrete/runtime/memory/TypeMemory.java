package org.evrete.runtime.memory;

import org.evrete.Configuration;
import org.evrete.api.*;
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
    private final IdentityMap map;
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
        this.map = new IdentityMap(conf);
        this.alphaBuckets = new ArrayOf<>(TypeMemoryBucket.class);
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void clear() {
        map.clear();
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.clear();
        }
    }

    public final FieldsMemory getCreate(FieldsKey fields) {
        return betaMemories
                .computeIfAbsent(fields, k -> new FieldsMemory(runtime, fields));
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
        map.forEachValue(consumer);
    }


    @Override
    public ReIterator<RuntimeFact> iterator() {
        return this.map.factIterator();
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

    public boolean init(AlphaMask alphaMask) {
        if (alphaMask.isEmpty()) {
            return false;
        } else {
            int bucketIndex = alphaMask.getBucketIndex();
            TypeMemoryBucket bucket;
            if (bucketIndex >= this.alphaBuckets.data.length) {
                bucket = new TypeMemoryBucket(runtime, alphaMask);
                this.alphaBuckets.append(bucket);
                return true;
            } else {
                bucket = this.alphaBuckets.data[bucketIndex];
                if (bucket == null) {
                    throw new IllegalStateException();
                } else {
                    return false;
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
        RuntimeFact rtf = map.remove(fact);
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
        map.forEachKey(f -> consumer.accept((T) f));
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        map.forEachKey(consumer);
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
                alphaTests[alpha.getIndex()] = alpha.test(values[fieldInUseIndex]);
            }
            rto = RuntimeObject.factory(o, values, alphaTests);
        } else {
            rto = RuntimeObject.factory(o, values);
        }

        if (map.put(o, rto) == null) {
            return rto;
        } else {
            LOGGER.warning("Object " + o + " has been already inserted, skipping insert");
            return null;
        }
    }

    private static class IdentityMap extends FastIdentityHashMap<Object, RuntimeFact> {
        private static final ToIntFunction<Object> HASH = System::identityHashCode;
        private static final Function<Entry<Object, RuntimeFact>, RuntimeFact> MAPPER = Entry::getValue;

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
