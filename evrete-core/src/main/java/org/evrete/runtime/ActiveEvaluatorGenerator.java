package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.events.ConditionEvaluationEvent;
import org.evrete.api.events.Events;
import org.evrete.collections.IndexingArrayMap;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * This class holds the conditions of the context and assigns them handles (see {@link EvaluatorHandle}).
 */
public class ActiveEvaluatorGenerator extends IndexingArrayMap<ActiveEvaluatorGenerator.InnerKey, ActiveEvaluatorGenerator.InnerKey, DefaultEvaluatorHandle, StoredCondition> implements Copyable<ActiveEvaluatorGenerator>, EvaluatorsContext {
    // Executor is required for evaluator publishers
    private final Executor executor;

    ActiveEvaluatorGenerator(Executor executor) {
        super(innerKey -> innerKey);
        this.executor = executor;
    }

    private ActiveEvaluatorGenerator(ActiveEvaluatorGenerator other) {
        super(other, StoredCondition::copyOf);
        this.executor = other.executor;
    }

    @Override
    protected DefaultEvaluatorHandle generateKey(InnerKey value, int index) {
        return new DefaultEvaluatorHandle(index, value.complexity);
    }

    @Override
    protected StoredCondition generateValue(DefaultEvaluatorHandle handle, InnerKey value) {
        return new StoredCondition(handle.getIndex(), value.predicate, value.complexity, value.fields);
    }

    @Override
    public ValuesPredicate getPredicate(EvaluatorHandle handle) {
        StoredCondition wrapper = get((DefaultEvaluatorHandle) handle, true);
        return wrapper == null ? null : wrapper.getCondition();
    }

    @Override
    public void replacePredicate(EvaluatorHandle handle, final ValuesPredicate newPredicate) {
        replace((DefaultEvaluatorHandle) handle, newPredicate);
    }

    /**
     * <p>
     * Registers new condition and returns its handle.
     * </p>
     *
     * @param predicate condition to add
     * @return new {@link EvaluatorHandle} or the one of an existing condition.
     */
    public synchronized DefaultEvaluatorHandle addEvaluator(ValuesPredicate predicate, double complexity, LhsField.Array<String, ActiveField> fields) {
        InnerKey key = new InnerKey(predicate, complexity, fields);
        MapEntry<DefaultEvaluatorHandle, StoredCondition> entry = getOrCreateEntry(key);
        return entry.getKey();
    }

    @Override
    @NonNull
    public Events.Publisher<ConditionEvaluationEvent> publisher(EvaluatorHandle handle) {
        return get((DefaultEvaluatorHandle) handle, false).getCreatePublisher(this.executor);
    }

    @Override
    public void forEach(BiConsumer<EvaluatorHandle, ValuesPredicate> consumer) {

        super.forEach(storedCondition -> consumer.accept(storedCondition.getHandle(), storedCondition.getCondition()));
    }

    private void replace(DefaultEvaluatorHandle handle, ValuesPredicate predicate) {
        get(handle, false).setPredicate(predicate);
    }

    public StoredCondition get(DefaultEvaluatorHandle handle, boolean returnNull) {
        StoredCondition result = super.get(handle);
        if (result == null) {
            if (returnNull) {
                return null;
            } else {
                throw new IllegalArgumentException("Unknown evaluator " + handle);
            }
        } else {
            return result;
        }
    }

    @Override
    public ActiveEvaluatorGenerator copyOf() {
        return new ActiveEvaluatorGenerator(this);
    }

    public static class InnerKey {
        private final ValuesPredicate predicate;
        private final double complexity;
        private final LhsField.Array<String, ActiveField> fields;

        private InnerKey(ValuesPredicate predicate, double complexity, LhsField.Array<String, ActiveField> fields) {
            this.predicate = predicate;
            this.complexity = complexity;
            this.fields = fields;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InnerKey key = (InnerKey) o;
            return Objects.equals(predicate, key.predicate);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(predicate);
        }
    }
}
