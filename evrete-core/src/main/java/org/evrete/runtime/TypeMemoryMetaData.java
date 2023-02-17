package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class TypeMemoryMetaData {
    private final int type;
    private final EvaluatorStorageImpl evaluators;
    private final MetaChangeListener listener;
    private final ArrayOf<FieldKeyMeta> keyMetas;
    private final AtomicInteger bucketCounter;
    private final AtomicInteger bucketIds;
    ActiveField[] activeFields;
    AlphaEvaluator[] alphaEvaluators;

    TypeMemoryMetaData(int type, EvaluatorStorageImpl evaluators, AtomicInteger bucketIds, MetaChangeListener listener) {
        this.type = type;
        this.activeFields = ActiveField.ZERO_ARRAY;
        this.alphaEvaluators = new AlphaEvaluator[0];
        this.evaluators = evaluators;
        this.listener = listener;
        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
        this.bucketCounter = new AtomicInteger(0);
        this.bucketIds = bucketIds;
    }

    private TypeMemoryMetaData(TypeMemoryMetaData other, EvaluatorStorageImpl evaluators, AtomicInteger bucketIds, MetaChangeListener listener) {
        this.activeFields = Arrays.copyOf(other.activeFields, other.activeFields.length);
        this.alphaEvaluators = Arrays.copyOf(other.alphaEvaluators, other.alphaEvaluators.length);
        this.type = other.type;
        this.evaluators = evaluators;
        this.listener = listener;
        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
        this.bucketCounter = new AtomicInteger(other.bucketCounter.get());
        this.bucketIds = bucketIds;
        other.keyMetas
                .forEach(
                        (meta, i) -> TypeMemoryMetaData.this.keyMetas.set(i, meta.copyOf())
                );

    }

    private synchronized AlphaEvaluator append(EvaluatorHandle wrapper, ActiveField[] descriptor) {
        int newId = this.alphaEvaluators.length;
        AlphaEvaluator alphaEvaluator = new AlphaEvaluator(newId, wrapper, descriptor);
        this.alphaEvaluators = Arrays.copyOf(this.alphaEvaluators, this.alphaEvaluators.length + 1);
        this.alphaEvaluators[newId] = alphaEvaluator;
        return alphaEvaluator;
    }

    TypeMemoryMetaData copyOf(EvaluatorStorageImpl evaluators, AtomicInteger bucketIds, MetaChangeListener listener) {
        return new TypeMemoryMetaData(this, evaluators, bucketIds, listener);
    }

    synchronized TypeMemoryMeta buildAlphaMask(FieldsKey key, Set<EvaluatorHandle> alphaEvaluators) {
        AlphaEvaluator[] existing = this.alphaEvaluators;
        Set<MatchedAlphaEvaluator> matches = new HashSet<>();
        for (EvaluatorHandle handle : alphaEvaluators) {
            MatchedAlphaEvaluator match = MatchedAlphaEvaluator.search(evaluators, existing, handle);
            if (match == null) {
                // No such evaluator, creating a new one
                AlphaEvaluator alphaEvaluator = this.append(handle, convertDescriptor(handle.descriptor()));
                existing = this.alphaEvaluators;
                matches.add(new MatchedAlphaEvaluator(alphaEvaluator, true));
            } else {
                matches.add(match);
            }
        }

        FieldKeyMeta fieldKeyMeta = getKeyMeta(key);

        for (TypeMemoryMeta meta : fieldKeyMeta.alphaBuckets) {
            if (meta.sameKey(matches)) {
                return meta;
            }
        }

        // Not found, creating a new one
        TypeMemoryMeta newMeta = fieldKeyMeta.newMeta(matches, bucketIds, bucketCounter);
        listener.onNewAlphaBucket(newMeta);
        return newMeta;
    }

    private ActiveField[] convertDescriptor(FieldReference[] descriptor) {
        ActiveField[] converted = new ActiveField[descriptor.length];
        for (int i = 0; i < descriptor.length; i++) {
            TypeField field = descriptor[i].field();
            ActiveField activeField = getCreate(field);
            converted[i] = activeField;
        }

        return converted;
    }

    synchronized ActiveField getCreate(TypeField field) {
        for (ActiveField af : activeFields) {
            if (af.getName().equals(field.getName())) {
                return af;
            }
        }
        // Create and store new instance
        int id = activeFields.length;
        ActiveField af = new ActiveField(field, id);
        this.activeFields = Arrays.copyOf(this.activeFields, id + 1);
        this.activeFields[id] = af;
        listener.onNewActiveField(af);
        return af;
    }

    private FieldKeyMeta getKeyMeta(FieldsKey key) {
        return keyMetas.computeIfAbsent(key.getId(), k -> new FieldKeyMeta(key));
    }


    private static class FieldKeyMeta implements Copyable<FieldKeyMeta> {
        private final FieldsKey fields;
        private TypeMemoryMeta[] alphaBuckets;

        FieldKeyMeta(FieldsKey fields) {
            this.fields = fields;
            this.alphaBuckets = new TypeMemoryMeta[0];
        }

        FieldKeyMeta(FieldKeyMeta other) {
            this.fields = other.fields;
            this.alphaBuckets = Arrays.copyOf(other.alphaBuckets, other.alphaBuckets.length);
        }

        synchronized TypeMemoryMeta newMeta(Set<MatchedAlphaEvaluator> matches, AtomicInteger bucketIds, AtomicInteger bucketCounter) {
            int idx = alphaBuckets.length;
            int bucketIndex = bucketCounter.getAndIncrement();
            int id = bucketIds.getAndIncrement();
            TypeMemoryMeta newMeta = TypeMemoryMeta.factory(id, bucketIndex, fields, matches);
            alphaBuckets = Arrays.copyOf(alphaBuckets, alphaBuckets.length + 1);
            alphaBuckets[idx] = newMeta;
            return newMeta;
        }

        @Override
        public FieldKeyMeta copyOf() {
            return new FieldKeyMeta(this);
        }
    }

    static class MatchedAlphaEvaluator {
        final AlphaEvaluator matched;
        final boolean direct;

        MatchedAlphaEvaluator(AlphaEvaluator matched, boolean direct) {
            this.matched = matched;
            this.direct = direct;
        }

        static TypeMemoryMetaData.MatchedAlphaEvaluator search(EvaluatorStorageImpl evaluators, AlphaEvaluator[] scope, EvaluatorHandle subject) {
            for (AlphaEvaluator evaluator : scope) {
                int cmp = evaluators.compare(evaluator.getDelegate(), subject);
                switch (cmp) {
                    case Evaluator.RELATION_EQUALS:
                        return new TypeMemoryMetaData.MatchedAlphaEvaluator(evaluator, true);
                    case Evaluator.RELATION_INVERSE:
                        return new TypeMemoryMetaData.MatchedAlphaEvaluator(evaluator, false);
                    case Evaluator.RELATION_NONE:
                        continue;
                    default:
                        throw new IllegalStateException();
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MatchedAlphaEvaluator match = (MatchedAlphaEvaluator) o;
            return direct == match.direct && matched.equals(match.matched);
        }

        @Override
        public int hashCode() {
            return matched.hashCode() + 37 * Boolean.hashCode(direct);
        }

    }

    public abstract static class TypeMemoryMeta implements MemoryAddress {
        private static final Set<MatchedAlphaEvaluator> EMPTY_COMPONENTS = new HashSet<>();
        private final int bucketIndex;
        private final int id;
        private final FieldsKey typeFields;
        private final Set<MatchedAlphaEvaluator> key;

        private TypeMemoryMeta(int id, int bucketIndex, FieldsKey fields, Set<MatchedAlphaEvaluator> matches) {
            this.id = id;
            this.key = matches;
            this.typeFields = fields;
            this.bucketIndex = bucketIndex;
        }

        static TypeMemoryMeta factory(int id, int bucketIndex, FieldsKey fields, Set<MatchedAlphaEvaluator> matches) {
            switch (matches.size()) {
                case 0:
                    return new Empty(id, bucketIndex, fields);
                case 1:
                    return new Single(id, bucketIndex, fields, matches);
                default:
                    return new Multi(id, bucketIndex, fields, matches);
            }
        }

        @Override
        public int getBucketIndex() {
            return bucketIndex;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public FieldsKey fields() {
            return typeFields;
        }

        final boolean sameKey(Set<MatchedAlphaEvaluator> other) {
            if (this.key.isEmpty() && other.isEmpty()) {
                return true;
            } else {
                return this.key.equals(other);
            }
        }

        @Override
        public final boolean isEmpty() {
            return this.key.isEmpty();
        }

        public static final class Empty extends TypeMemoryMeta {

            Empty(int id, int bucketIndex, FieldsKey fields) {
                super(id, bucketIndex, fields, EMPTY_COMPONENTS);
            }

            @Override
            public boolean testAlphaBits(BitSet mask) {
                return true;
            }
        }

        public static final class Single extends TypeMemoryMeta {
            private final int bitIndex;
            private final boolean expectedValue;

            Single(int id, int bucketIndex, FieldsKey fields, Set<MatchedAlphaEvaluator> matches) {
                super(id, bucketIndex, fields, matches);
                MatchedAlphaEvaluator match = matches.iterator().next();
                this.expectedValue = match.direct;
                this.bitIndex = match.matched.getIndex();
            }

            @Override
            public boolean testAlphaBits(BitSet mask) {
                return mask.get(bitIndex) == expectedValue;
            }
        }

        public static final class Multi extends TypeMemoryMeta {
            private final int[] bitIndices;
            private final BitSet expectedValues = new BitSet();

            Multi(int id, int bucketIndex, FieldsKey fields, Set<MatchedAlphaEvaluator> matches) {
                super(id, bucketIndex, fields, matches);
                List<MatchedAlphaEvaluator> sortedMatches = new ArrayList<>(matches);
                sortedMatches.sort(Comparator.comparingDouble(o -> o.matched.getDelegate().getComplexity()));
                this.bitIndices = new int[sortedMatches.size()];
                int i = 0;
                for (MatchedAlphaEvaluator match : sortedMatches) {
                    this.bitIndices[i] = match.matched.getIndex();
                    if (match.direct) {
                        this.expectedValues.set(match.matched.getIndex());
                    }
                    i++;
                }
            }

            @Override
            public boolean testAlphaBits(BitSet mask) {
                for (int idx : bitIndices) {
                    if (mask.get(idx) != expectedValues.get(idx)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "{bucket=" + getBucketIndex() +
                        ", values=" + expectedValues +
                        '}';
            }
        }
    }
}
