package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.collections.ForkingArrayMap;
import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * <p>
 * Runtime context's base class containing methods for defining and accessing active (in use) information
 * about fact types, conditions, and listeners. This information is automatically copied to dependent (child)
 * context, e.g. when creating sessions from a parent (Knowledge) context.
 * </p>
 *
 * @param <C> contact type
 */
abstract class AbstractRuntimeMeta<C extends RuntimeContext<C>> extends AbstractRuntimeBase<C> {
    private final ActiveEvaluatorGenerator evaluatorGenerator;
    private final ActiveTypeGenerator activeTypeGenerator;
    private final AlphaAddressIndexer alphaAddressIndexer;

    AbstractRuntimeMeta(KnowledgeService service) {
        super(service);
        this.evaluatorGenerator = new ActiveEvaluatorGenerator(service.getExecutor());
        this.activeTypeGenerator = new ActiveTypeGenerator();
        this.alphaAddressIndexer = new AlphaAddressIndexer();
    }

    AbstractRuntimeMeta(AbstractRuntimeMeta<?> parent) {
        super(parent);
        this.evaluatorGenerator = parent.evaluatorGenerator.copyOf();
        this.activeTypeGenerator = parent.activeTypeGenerator.copyOf();
        this.alphaAddressIndexer = parent.alphaAddressIndexer.copyOf();
    }

    @Override
    public ActiveEvaluatorGenerator getEvaluatorsContext() {
        return this.evaluatorGenerator;
    }

    ActiveField getCreateActiveField(TypeField field) {
        return this.activeTypeGenerator.getOrCreateEntry(field.getDeclaringType()).getValue().getCreateActiveField(field);
    }

    @NonNull
    ActiveType getCreateIndexedType(Type<?> type) {
        return this.activeTypeGenerator.getOrCreateEntry(type).getValue();
    }

    AlphaAddress getCreateAlphaAddress(TypeAlphaConditions alphaConditions) {
        return this.alphaAddressIndexer.getOrCreateEntry(alphaConditions).getValue();
    }

    /**
     * Builds a {@link FactType} from a given LHS builder's fact declaration. The fact's type and fields
     * are indexed, and a new {@link AlphaAddress} is assigned to the result.
     *
     * @param lhsFact the instance of the fact used by the builder
     * @param alphaHandles the alpha-conditions bound to the fact
     * @return a new instance of {@link FactType}
     */
    FactType buildFactType(DefaultLhsBuilder.Fact lhsFact, Set<DefaultEvaluatorHandle> alphaHandles) {
        // 1. Convert the fact's type into an active type
        ActiveType activeType = getCreateIndexedType(lhsFact.getType());

        // 2. Index alpha conditions
        TypeAlphaConditions indexedAlphaConditions = activeType.getCreateAlphaConditions(alphaHandles);

        // 3. Find or create alpha address for the conditions
        AlphaAddress alphaAddress = getCreateAlphaAddress(indexedAlphaConditions);

        // 4. Let the active type know about the alpha address
        activeType.registerAlphaAddress(alphaAddress);

        return new FactType(lhsFact, activeType, alphaAddress);
    }

    Stream<ActiveType> activeTypes() {
        return this.activeTypeGenerator.values();
    }

    ActiveType getActiveType(DefaultFactHandle handle) {
        return getActiveType(handle.getType());
    }

    ActiveType getActiveType(ActiveType.Idx id) {
        return this.activeTypeGenerator.get(id);
    }

    LhsField.Array<String, ActiveField> toActiveFields(LhsField.Array<String, TypeField> fields) {
        return fields.transform(f -> new LhsField<>(f, getCreateActiveField(f.field())));
    }

//    Stream<AlphaConditionHandle> alphaConditionHandles(ActiveType.Idx typeId) {
//        return getActiveType(typeId).getAlphaConditions();
//    }

    void forEachAlphaConditionHandle(ActiveType.Idx typeId, Consumer<AlphaConditionHandle> consumer) {
        getActiveType(typeId).forEachValue(consumer);
    }


    private static class AlphaAddressIndexer extends ForkingArrayMap<TypeAlphaConditions, TypeAlphaConditions, AlphaAddress, AlphaAddress> implements Copyable<AlphaAddressIndexer>{

        AlphaAddressIndexer() {
            super(set -> set);
        }

        private AlphaAddressIndexer(AlphaAddressIndexer other) {
            super(other);
        }

        @Override
        public AlphaAddressIndexer copyOf() {
            return new AlphaAddressIndexer(this);
        }

        @Override
        protected AlphaAddress generateKey(TypeAlphaConditions value, int index) {
            return new AlphaAddress(index, value);
        }

        @Override
        protected AlphaAddress generateValue(AlphaAddress idx, TypeAlphaConditions value) {
            return idx;
        }
    }

}
