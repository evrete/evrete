package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.structure.*;

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


    public RuntimeAggregateLhsJoined(RuntimeRule rule, RuntimeLhs parent, AggregateLhsDescriptor descriptor) {
        super(rule, parent, descriptor);
        int allFactTypes = rule.getAllFactTypes().length;
        this.descriptor = descriptor;

        AggregateEvaluator aggregateEvaluator = descriptor.getJoinCondition();

        this.evaluators = descriptor.getJoinCondition().getEvaluators();
        this.evaluatorValues = new IntToValue[evaluators.length];


        RuntimeLhs[] allLhs = new RuntimeLhs[]{parent, this};
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
                this.iterators[iteratorIndex] = new SourceIterator(iteratorIndex, d, allLhs[level].resolve(d));
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
            it = iterator.main();
            it.reset();
            while (it.hasNext() && (!deltaOnly || hasDelta)) {
                state[index] = it.next();
                testAndSave();
            }

            // 2. Delta
            it = iterator.delta();
            it.reset();
            while (it.hasNext()) {
                state[index] = it.next();
                testAndSave();
            }

        } else {
            // 1. Main
            it = iterator.main();
            it.reset();
            while (it.hasNext()) {
                state[index] = it.next();
                evaluate(index + 1, hasDelta, deltaOnly);
            }

            // 2. Delta
            it = iterator.delta();
            it.reset();
            while (it.hasNext()) {
                state[index] = it.next();
                evaluate(index + 1, true, deltaOnly);
            }
        }
    }

    void testAndSave() {
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


    static class SourceIterator {
        private final FactType[] types;
        private final int index;
        private final ReIterator<ValueRow[]> main;
        private final ReIterator<ValueRow[]> delta;

        SourceIterator(int index, RhsFactGroupDescriptor descriptor, RhsKeysGroupIterator iterator) {
            this.types = descriptor.getTypes();
            this.index = index;

            this.main = iterator.getMainIterator();
            this.delta = iterator.getDeltaIterator();
        }

        ReIterator<ValueRow[]> main() {
            return main;
        }

        ReIterator<ValueRow[]> delta() {
            return delta;
        }
    }
}
