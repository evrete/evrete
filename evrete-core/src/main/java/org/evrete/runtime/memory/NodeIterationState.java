package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationState;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.ConditionNodeDescriptor;
import org.evrete.runtime.FactType;
import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;
import org.evrete.util.CollectionUtils;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public class NodeIterationState implements NodeIterationStateFactory.State, BetaEvaluationState {
    private final KeysStore.Entry[][] state;
    private final IntFunction<IntToValueRow> destinationValues;
    private final int[] nonPlainSourceIndices;
    private final ReIterator<KeysStore.Entry>[] secondary;
    private final EvaluatorDelegate[] evaluators;
    private final boolean nonPlainSources;


    @SuppressWarnings("unchecked")
    public NodeIterationState(BetaConditionNode node) {
        BetaMemoryNode<?>[] sources = node.getSources();


        this.nonPlainSourceIndices = node.getNonPlainSourceIndices();
        this.nonPlainSources = nonPlainSourceIndices.length > 0;


        // Destination data
        FactType[][] primaryFactTypes = new FactType[sources.length][];
        for (BetaMemoryNode<?> source : sources) {
            primaryFactTypes[source.getSourceIndex()] = source.getGrouping()[0];
        }


        FactType[][] secondaryFactTypes = new FactType[nonPlainSourceIndices.length][];
        for (int z = 0; z < nonPlainSourceIndices.length; z++) {
            int nonPlainSourceId = nonPlainSourceIndices[z];
            BetaMemoryNode<?> source = sources[nonPlainSourceId];
            secondaryFactTypes[z] = source.getGrouping()[1];
        }


        FactType[][] grouping = node.getGrouping();
        int totalLevels = grouping.length;
        int[][][] destinationData = new int[totalLevels][][];

        for (int level = 0; level < totalLevels; level++) {
            FactType[] levelTypes = grouping[level];
            int[][] locations = new int[levelTypes.length][];

            for (int typeArrIndex = 0; typeArrIndex < levelTypes.length; typeArrIndex++) {
                FactType t = levelTypes[typeArrIndex];

                int[] addr;
                int[] loc = CollectionUtils.locate2(t, primaryFactTypes, FactType.EQUALITY_BY_INDEX);
                if (loc != null) {
                    // Type belongs to primary level
                    addr = new int[]{0, loc[0], loc[1]};
                } else {
                    loc = CollectionUtils.locate2(t, secondaryFactTypes, FactType.EQUALITY_BY_INDEX);
                    if (loc == null) {
                        //This can happen if one of the sources has grouping size deeper than 2
                        throw new IllegalStateException();
                    }
                    addr = new int[]{1, loc[0], loc[1]};
                }
                locations[typeArrIndex] = addr;
            }

            destinationData[level] = locations;
        }


        if (this.nonPlainSourceIndices.length == 0) {
            this.state = new KeysStore.Entry[1][];
        } else {
            this.state = new KeysStore.Entry[2][];
            this.state[1] = new KeysStore.Entry[this.nonPlainSourceIndices.length];
        }
        this.state[0] = new KeysStore.Entry[sources.length];


        this.destinationValues = new DestinationValueAdapter(state, destinationData);
        //this.conditionValues = new ConditionValueAdapter(state[0], conditionMappingIndices);
        this.secondary = (ReIterator<KeysStore.Entry>[]) (new ReIterator[this.nonPlainSourceIndices.length]);


        BetaEvaluatorGroup inner = node.getExpression();

        BetaEvaluator[] betaEvaluators = inner.getEvaluators();

        this.evaluators = new EvaluatorDelegate[betaEvaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            this.evaluators[i] = new EvaluatorDelegate(betaEvaluators[i], state[0], node);
        }

    }

    @Override
    public void saveTo(KeysStore destination) {
        destination.save(destinationValues);
    }

    @Override
    public boolean evaluate() {
        for (EvaluatorDelegate ed : evaluators) {
            if (!ed.test()) return false;
        }
        return true;
    }

    @Override
    public Object apply(FactType factType, int fieldIndex) {
        //TODO override or provide a message
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNonPlainSources() {
        return nonPlainSources;
    }

    @Override
    public void setEvaluationEntry(KeysStore.Entry entry, int sourceId) {
        state[0][sourceId] = entry;
    }

    @Override
    public void setSecondaryEntry(KeysStore.Entry entry, int nonPlainIndex) {
        state[1][nonPlainIndex] = entry;
    }

    @ThreadUnsafe
    @Override
    public ReIterator<KeysStore.Entry>[] buildSecondary() {
        for (int i = 0; i < nonPlainSourceIndices.length; i++) {
            int sourceId = nonPlainSourceIndices[i];
            secondary[i] = state[0][sourceId].getNext().entries();
        }
        return secondary;
    }

    private static class DestinationValueAdapter implements IntFunction<IntToValueRow> {
        private final LevelMapper[] levelMappers;

        DestinationValueAdapter(KeysStore.Entry[][] currentState, int[][][] locationData) {
            this.levelMappers = new LevelMapper[locationData.length];
            for (int level = 0; level < locationData.length; level++) {
                this.levelMappers[level] = new LevelMapper(currentState, locationData[level]);
            }
        }

        @Override
        public IntToValueRow apply(int level) {
            return levelMappers[level];
        }

        private static class LevelMapper implements IntToValueRow {
            private final KeysStore.Entry[][] currentState;

            private final int[][] locations;

            LevelMapper(KeysStore.Entry[][] currentState, int[][] locations) {
                this.currentState = currentState;
                this.locations = locations;
            }

            @Override
            public ValueRow apply(int typeArrIndex) {
                int[] addr = locations[typeArrIndex];
                return currentState[addr[0]][addr[1]].key()[addr[2]];//apply(addr[2]);
            }
        }
    }

    private final static class EvaluatorDelegate {
        final BetaEvaluator evaluator;
        final IntToValue mappedValues;
        final ValueSupplier[] values;
        final BetaConditionNode node;

        EvaluatorDelegate(BetaEvaluator evaluator, KeysStore.Entry[] state, BetaConditionNode node) {
            this.evaluator = evaluator;
            this.node = node;
            this.values = new ValueSupplier[evaluator.betaDescriptor().length];
            for (int refId = 0; refId < evaluator.betaDescriptor().length; refId++) {
                BetaFieldReference ref = evaluator.betaDescriptor()[refId];
                FactType type = ref.getFactType();

                int fieldIndex = ref.getFieldIndex();
                ConditionNodeDescriptor.TypeLocator loc = node.getDescriptor().locate(type);
                assert loc.level == 0;
                int sourceIndex = loc.source;
                int factIndex = loc.position;
                //this.locations[refId] = new int[]{sourceIndex, factIndex, fieldIndex};
                this.values[refId] = new ValueSupplier(state, sourceIndex, factIndex, fieldIndex);
            }

            this.mappedValues = refId -> values[refId].get();
        }


        boolean test() {
            return evaluator.test(mappedValues);
        }

        @Override
        public String toString() {
            return "EvaluatorDelegate{" +
                    "evaluator=" + evaluator +
                    '}';
        }
    }


    private static class ValueSupplier implements Supplier<Object> {
        private final KeysStore.Entry[] state;
        private final int sourceId;
        private final int typeId;
        private final int fieldId;

        ValueSupplier(KeysStore.Entry[] state, int sourceId, int typeId, int fieldId) {
            this.state = state;
            this.sourceId = sourceId;
            this.typeId = typeId;
            this.fieldId = fieldId;
        }

        @Override
        public Object get() {
            return state[sourceId].key()[typeId].get(fieldId);
        }
    }
}
