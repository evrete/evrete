package org.evrete.runtime;

import org.evrete.api.Copyable;
import org.evrete.api.EvaluatorHandle;
import org.evrete.api.FieldReference;
import org.evrete.api.TypeField;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class TypeMemoryMetaData {
    //final Type<?> type;
    final int type;
    private final Evaluators evaluators;
    private final MetaChangeListener listener;
    private final ArrayOf<FieldKeyMeta> keyMetas;
    ActiveField[] activeFields;
    AlphaEvaluator[] alphaEvaluators;


    TypeMemoryMetaData(int type, Evaluators evaluators, MetaChangeListener listener) {
        this.type = type;
        this.activeFields = ActiveField.ZERO_ARRAY;
        this.alphaEvaluators = new AlphaEvaluator[0];
        this.evaluators = evaluators;
        this.listener = listener;
        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
    }

    private TypeMemoryMetaData(TypeMemoryMetaData other, Evaluators evaluators, MetaChangeListener listener) {
        this.activeFields = Arrays.copyOf(other.activeFields, other.activeFields.length);
        this.alphaEvaluators = Arrays.copyOf(other.alphaEvaluators, other.alphaEvaluators.length);
        this.type = other.type;
        this.evaluators = evaluators;
        this.listener = listener;
        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
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

    TypeMemoryMetaData copyOf(Evaluators evaluators, MetaChangeListener listener) {
        return new TypeMemoryMetaData(this, evaluators, listener);
    }

    synchronized AlphaBucketMeta buildAlphaMask(FieldsKey key, Set<EvaluatorHandle> alphaEvaluators) {
        AlphaEvaluator[] existing = this.alphaEvaluators;
        Set<AlphaEvaluator.Match> matches = new HashSet<>();
        for (EvaluatorHandle handle : alphaEvaluators) {
            AlphaEvaluator.Match match = AlphaEvaluator.search(evaluators, existing, handle);
            if (match == null) {
                // No such evaluator, creating a new one
                AlphaEvaluator alphaEvaluator = this.append(handle, convertDescriptor(handle.descriptor()));
                existing = this.alphaEvaluators;
                matches.add(new AlphaEvaluator.Match(alphaEvaluator, true));
            } else {
                matches.add(match);
            }
        }

        // Now that all evaluators are matched,
        // their unique combinations are converted to a alpha bucket meta-data
        FieldKeyMeta fieldKeyMeta = getKeyMeta(key);

        for (AlphaBucketMeta meta : fieldKeyMeta.alphaBuckets) {
            if (meta.sameKey(matches)) {
                return meta;
            }
        }

        // Not found creating a new one
        int bucketIndex = fieldKeyMeta.alphaBuckets.length;
        AlphaBucketMeta newMeta = AlphaBucketMeta.factory(bucketIndex, matches);
        fieldKeyMeta.alphaBuckets = Arrays.copyOf(fieldKeyMeta.alphaBuckets, fieldKeyMeta.alphaBuckets.length + 1);
        fieldKeyMeta.alphaBuckets[bucketIndex] = newMeta;

        listener.onNewAlphaBucket(type, key, newMeta);
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
            if (af.field() == field.getId()) {
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
        return keyMetas.computeIfAbsent(key.getId(), k -> new FieldKeyMeta());
    }


    private static class FieldKeyMeta implements Copyable<FieldKeyMeta> {
        private AlphaBucketMeta[] alphaBuckets;

        FieldKeyMeta() {
            this.alphaBuckets = new AlphaBucketMeta[0];
        }

        FieldKeyMeta(FieldKeyMeta other) {
            this.alphaBuckets = Arrays.copyOf(other.alphaBuckets, other.alphaBuckets.length);
        }

        @Override
        public FieldKeyMeta copyOf() {
            return new FieldKeyMeta(this);
        }
    }

}
