package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.ForkingArrayMap;
import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.evrete.util.AbstractIndex;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * <p>
 * Active types are auto-indexed instances created from those {@link Type} instances that are actually
 * used by rules. Created, but unused, {@link Type} declarations will not be indexed.
 * </p>
 */
public class ActiveType implements Copyable<ActiveType> {
    private final ActiveFields activeFields;
    private final AlphaConditionIndexer alphaConditionIndexer;
    private final AlphaConditionSubsetIndexer alphaConditionSubsetIndexer;
    private final Type<?> value;
    private final Idx id;
    private final Set<AlphaAddress> knownAlphaLocations;

    public ActiveType(Idx id, Type<?> value) {
        this.value = value;
        this.id = id;
        this.activeFields = new ActiveFields(id);
        this.alphaConditionIndexer = new AlphaConditionIndexer();
        this.alphaConditionSubsetIndexer = new AlphaConditionSubsetIndexer(id);
        this.knownAlphaLocations = new HashSet<>();
    }

    public Type<?> getValue() {
        return value;
    }

    public Idx getId() {
        return id;
    }

    /**
     * Deep copy constructor which is invoked by the {@link #copyOf()} method.
     * See {@link Copyable} for details about copying.
     *
     * @param parent the source instance
     */
    private ActiveType(ActiveType parent) {
        this.value = parent.value.copyOf();
        this.id = parent.id;
        this.activeFields = parent.activeFields.newBranch();
        this.alphaConditionIndexer = parent.alphaConditionIndexer.copyOf();
        this.alphaConditionSubsetIndexer = parent.alphaConditionSubsetIndexer.copyOf();
        this.knownAlphaLocations = new HashSet<>(parent.knownAlphaLocations);
    }

    void registerAlphaAddress(AlphaAddress alphaAddress) {
        this.knownAlphaLocations.add(alphaAddress);
    }

    Mask<AlphaConditionHandle> alphaConditionResults(FactFieldValues values, AbstractRuleSession<?> runtime) {
        Mask<AlphaConditionHandle> alphaConditionResults = Mask.alphaConditionsMask();
        ActiveEvaluatorGenerator context = runtime.getEvaluatorsContext();
        this.forEachAlphaCondition(indexedHandle -> {
            StoredCondition evaluator = context.get(indexedHandle.getHandle(), false);
            ActiveField activeField = evaluator.getDescriptor().get(0).field();
            IntToValue args = index -> values.valueAt(activeField.valueIndex());
            alphaConditionResults.set(indexedHandle, evaluator.test(runtime, args));
        });

        return alphaConditionResults;
    }

    Collection<AlphaAddress> matchingLocations(AbstractRuleSession<?> runtime, FactFieldValues fieldValues, Set<AlphaAddress> scope) {
        return AlphaAddress.matchingLocations(alphaConditionResults(fieldValues, runtime), scope);
    }

    Collection<AlphaAddress> matchingLocations(AbstractRuleSession<?> runtime, FactFieldValues fieldValues) {
        return AlphaAddress.matchingLocations(alphaConditionResults(fieldValues, runtime), this.knownAlphaLocations);
    }


    public Set<AlphaAddress> getKnownAlphaLocations() {
        return Collections.unmodifiableSet(knownAlphaLocations);
    }

    public Stream<AlphaConditionHandle> getAlphaConditions() {
        return alphaConditionIndexer.values();
    }

    TypeAlphaConditions getCreateAlphaConditions(Set<DefaultEvaluatorHandle> alphaConditions) {
        // Index alpha conditions
        Set<AlphaConditionHandle> indexedAlphaConditions = new HashSet<>(alphaConditions.size());
        for (DefaultEvaluatorHandle alphaCondition : alphaConditions) {
            MapEntry<AlphaConditionHandle, AlphaConditionHandle> entry = this.alphaConditionIndexer.getOrCreateEntry(alphaCondition);
            indexedAlphaConditions.add(entry.getValue());
        }
        // Obtain a unique identifier for the provided set of conditions
        return alphaConditionSubsetIndexer.getOrCreateEntry(indexedAlphaConditions).getValue();
    }

    FactFieldValues readFactValue(Type<?> type, Object fact) {
        final Object[] values = new Object[activeFields.size()];
        activeFields.forEachValue(new Consumer<ActiveField>() {
            @Override
            public void accept(ActiveField activeField) {
                TypeField field = type.getField(activeField.getName());
                values[activeField.valueIndex()] = field.readValue(fact);;
            }
        });
        return new FactFieldValues(values);
    }

    ActiveField getCreateActiveField(TypeField field) {
        return this.activeFields.getOrCreateEntry(field).getValue();
    }

    int getCountOfAlphaConditions() {
        return alphaConditionIndexer.size();
    }

    int getFieldCount() {
        return activeFields.size();
    }

    void forEachAlphaCondition(Consumer<AlphaConditionHandle> action) {
        this.alphaConditionIndexer.forEachValue(action);
    }

    void forEachAlphaAddress(Consumer<AlphaAddress> action) {
        this.knownAlphaLocations.forEach(action);
    }

    @Override
    public ActiveType copyOf() {
        return new ActiveType(this);
    }

    @Override
    public String toString() {
        return "{id=" + getId() +
                ", name='" + getValue().getName() + "'" +
                ", fieldCount=" + activeFields.size() +
                '}';
    }

    public static class Idx extends AbstractIndex implements Serializable {
        private static final long serialVersionUID = 6171956208382559856L;

        public Idx(int index) {
            super(index, index);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Idx that = (Idx) o;
            return getIndex() == that.getIndex();
        }
    }

    private static class AlphaConditionSubsetIndexer extends ForkingArrayMap<Set<AlphaConditionHandle>, Set<AlphaConditionHandle>, TypeAlphaConditions, TypeAlphaConditions> implements Copyable<AlphaConditionSubsetIndexer>{
        private final ActiveType.Idx type;

        public AlphaConditionSubsetIndexer(ActiveType.Idx type) {
            super(set -> set);
            this.type = type;
        }

        public AlphaConditionSubsetIndexer(AlphaConditionSubsetIndexer other, Idx type) {
            super(other);
            this.type = type;
        }

        @Override
        protected TypeAlphaConditions generateKey(Set<AlphaConditionHandle> value, int index) {
            return new TypeAlphaConditions(index, type, value);
        }

        @Override
        protected TypeAlphaConditions generateValue(TypeAlphaConditions idx, Set<AlphaConditionHandle> value) {
            return idx;
        }

        @Override
        public AlphaConditionSubsetIndexer copyOf() {
            return new AlphaConditionSubsetIndexer(this, type);
        }
    }


    /**
     * This class provides indexed storage of the type's alpha conditions. When facts are inserted or updated,
     * we'll create a {@link Mask} where each bit position will hold the result of the evaluation
     * of the fact's values against the corresponding condition.
     * This class provides mapping between conditions and their unique int indexes in bit masks.
     */
    private static class AlphaConditionIndexer extends ForkingArrayMap<DefaultEvaluatorHandle, Integer, AlphaConditionHandle, AlphaConditionHandle> implements Copyable<AlphaConditionIndexer> {

        AlphaConditionIndexer() {
            super(DefaultEvaluatorHandle::getIndex);
        }

        AlphaConditionIndexer(AlphaConditionIndexer other) {
            super(other);
        }

        @Override
        protected AlphaConditionHandle generateKey(DefaultEvaluatorHandle value, int index) {
            return new AlphaConditionHandle(index, value);
        }

        @Override
        protected AlphaConditionHandle generateValue(AlphaConditionHandle idx, DefaultEvaluatorHandle value) {
            return idx;
        }

        @Override
        public AlphaConditionIndexer copyOf() {
            return new AlphaConditionIndexer(this);
        }
    }

    private static class ActiveFields extends ForkingArrayMap<TypeField, String, ActiveField.Index, ActiveField> implements Branchable<ActiveFields> {
        final Idx typeId;

        ActiveFields(Idx typeId) {
            super(Named::getName);
            this.typeId = typeId;
        }

        ActiveFields(ActiveFields parent) {
            super(parent);
            this.typeId = parent.typeId;
        }

        @Override
        protected ActiveField.Index generateKey(TypeField value, int index) {
            return new ActiveField.Index(index);
        }

        @Override
        protected ActiveField generateValue(ActiveField.Index index, TypeField value) {
            return new ActiveField(typeId, value, index.getIndex());
        }

        @Override
        public ActiveFields newBranch() {
            return new ActiveFields(this);
        }
    }
}
