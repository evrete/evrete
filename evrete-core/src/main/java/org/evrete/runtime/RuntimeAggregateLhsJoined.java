package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.EvaluatorInternal;

import java.util.EnumMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

public class RuntimeAggregateLhsJoined extends RuntimeAggregateLhs {
    private final ValueRow[][] state;
    private final SourceIterator[] iterators;
    private final EvaluatorInternal[] evaluators;
    private final IntToValue[] evaluatorValues;
    private final KeysStore successData;
    private final IntFunction<IntToValueRow> saveMapper;
    private final BooleanSupplier aggregateKeyPredicate;
    private final int[][][] saveMapping;
    private final AggregateLhsDescriptor descriptor;


    public RuntimeAggregateLhsJoined(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AggregateLhsDescriptor descriptor) {
        super(rule, parent, descriptor);
        int allFactTypes = rule.getAllFactTypes().length;
        this.descriptor = descriptor;

        AggregateEvaluator aggregateEvaluator = descriptor.getJoinCondition();

        this.evaluators = descriptor.getJoinCondition().getEvaluators();
        this.evaluatorValues = new IntToValue[evaluators.length];


        AbstractRuntimeLhs[] allLhs = new AbstractRuntimeLhs[]{parent, this};
        this.saveMapping = new int[allLhs.length][][];


        // Pass 1. Collect key allocation data
        AggregateEvaluator.LevelData[] levelData = aggregateEvaluator.getLevelData();
        int[] groupingSizes = new int[levelData.length];
        int totalGroups = 0;
        for (int level = 0; level < levelData.length; level++) {
            AggregateEvaluator.LevelData data = levelData[level];
            FactType[] types = data.getFactTypeSequence();
            saveMapping[level] = new int[types.length][];
            groupingSizes[level] = types.length;
            totalGroups += data.getKeyGroupSequence().length;
        }

        this.iterators = new SourceIterator[totalGroups];
        this.state = new ValueRow[iterators.length][];


        int inLevelIndex, iteratorIndex = 0;

        for (int level = 0; level < levelData.length; level++) {
            AggregateEvaluator.LevelData data = levelData[level];
            inLevelIndex = 0;
            RhsFactGroupDescriptor[] groupDescriptors = data.getKeyGroupSequence();
            for (RhsFactGroupDescriptor d : groupDescriptors) {
                FactType[] types = d.getTypes();
                for (FactType t : types) {
                    saveMapping[level][inLevelIndex] = new int[]{iteratorIndex, t.getInGroupIndex()};
                    inLevelIndex++;
                }
                AbstractRuntimeLhs lhs = allLhs[level];
                RhsFactGroupBeta beta = lhs.getGroup(d);
                this.iterators[iteratorIndex] = new SourceIterator(iteratorIndex, d, beta);
                iteratorIndex++;
            }
        }


        // Create success store

        this.successData = rule.getMemory().newKeysStore(groupingSizes);

        // Create save mapping
        this.saveMapper = level -> typeIndex -> {
            int[] addr = saveMapping[level][typeIndex];
            return state[addr[0]][addr[1]];
        };

        // Create evaluation mapping
        final int[][] conditionMapping = new int[allFactTypes][];
        for (SourceIterator iterator : iterators) {
            for (int t = 0; t < iterator.types.length; t++) {
                FactType type = iterator.types[t];
                int typeId = type.getInRuleIndex();
                assert conditionMapping[typeId] == null;
                conditionMapping[typeId] = new int[]{iterator.index, t};
            }
        }


        for (int i = 0; i < evaluators.length; i++) {
            this.evaluatorValues[i] = new IntToValueImpl(evaluators[i], state, conditionMapping);
        }


        this.aggregateKeyPredicate = descriptor.getAggregateEvaluatorFactory().joinedGroupEvaluator(parent, this);

    }

    public BooleanSupplier getAggregateKeyPredicate() {
        return aggregateKeyPredicate;
    }

    public KeysStore getSuccessData() {
        return successData;
    }

    public AggregateLhsDescriptor getDescriptor() {
        return descriptor;
    }

    public void evaluate(boolean deltaOnly) {
        evaluate(0, false, deltaOnly);
    }

    private void evaluate(int index, boolean hasDelta, boolean deltaOnly) {
        SourceIterator iterator = iterators[index];
        ReIterator<ValueRow[]> it;
        if (index == iterators.length - 1) {
            // Last
            // 1. Main
            it = iterator.keyIterator(KeyMode.KNOWN_KEYS_KNOWN_FACTS);
            if (it.reset() > 0) {
                while (it.hasNext() && (!deltaOnly || hasDelta)) {
                    state[index] = it.next();
                    testAndSave();
                }
            }

            // 2. Delta
            it = iterator.keyIterator(KeyMode.NEW_KEYS_NEW_FACTS);
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    state[index] = it.next();
                    testAndSave();
                }
            }

        } else {
            // 1. Main
            it = iterator.keyIterator(KeyMode.KNOWN_KEYS_KNOWN_FACTS);
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    state[index] = it.next();
                    evaluate(index + 1, hasDelta, deltaOnly);
                }
            }

            // 2. Delta
            it = iterator.keyIterator(KeyMode.NEW_KEYS_NEW_FACTS);
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    state[index] = it.next();
                    evaluate(index + 1, true, deltaOnly);
                }
            }
        }
    }

    private void testAndSave() {
        for (int i = 0; i < evaluators.length; i++) {
            if (!evaluators[i].test(evaluatorValues[i])) {
                return;
            }
        }
        this.successData.save(saveMapper);
    }

    private static class IntToValueImpl implements IntToValue {
        private final ValueRow[][] state;
        private final int[][] addr;

        IntToValueImpl(EvaluatorInternal evaluator, ValueRow[][] state, int[][] conditionMapping) {
            this.state = state;
            FactTypeField[] descriptor = evaluator.descriptor();
            this.addr = new int[descriptor.length][];


            for (int value = 0; value < this.addr.length; value++) {
                FactTypeField ref = descriptor[value];
                FactType factType = ref.getFactType();
                int[] map = conditionMapping[factType.getInRuleIndex()];
                this.addr[value] = new int[]{map[0], map[1], ref.getFieldIndex()};
            }
        }

        @Override
        public Object apply(int value) {
            int[] map = this.addr[value];
            return state[map[0]][map[1]].get(map[2]);
        }
    }


    static class SourceIterator implements KeyReIterators<ValueRow[]> {
        private final FactType[] types;
        private final int index;
        private final EnumMap<KeyMode, ReIterator<ValueRow[]>> map;

        SourceIterator(int index, RhsFactGroupDescriptor descriptor, KeyReIterators<ValueRow[]> iterator) {
            this.types = descriptor.getTypes();
            this.index = index;
            this.map = iterator.keyIterators();
        }

        @Override
        public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
            return map;
        }
    }
}
