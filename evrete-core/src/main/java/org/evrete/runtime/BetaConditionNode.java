package org.evrete.runtime;

import org.evrete.api.*;

import java.util.Arrays;
import java.util.function.IntFunction;

public class BetaConditionNode extends AbstractBetaConditionNode {
    static final BetaConditionNode[] EMPTY_ARRAY = new BetaConditionNode[0];
    private final ReIterator<KeysStore.Entry>[] primaryKeyIterators;
    private final ReIterator<KeysStore.Entry>[] secondaryKeyIterators;
    private final ValueRow[] evaluationState;
    private final SourceMeta[] sourceMetas;
    private final IntFunction<IntToValueRow> saveFunction;
    private final FactType[][] secondaryTypes;
    private final KeyMode[] currentSourceModes;
    private final int[] secondary2source;


    @SuppressWarnings("unchecked")
    BetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode<?>[] sources) {
        super(rule, descriptor, sources);
        this.evaluationState = new ValueRow[rule.getAllFactTypes().length];
        this.currentSourceModes = new KeyMode[getSources().length];
        //TODO !!! optimize
        this.saveFunction = level -> {
            FactType[] levelTypes = getGrouping()[level];
            return new IntToValueRow() {
                @Override
                public ValueRow apply(int value) {
                    return evaluationState[levelTypes[value].getInRuleIndex()];
                }
            };
        };
        this.sourceMetas = new SourceMeta[sources.length];
        this.secondary2source = new int[sources.length];
        Arrays.fill(secondary2source, -1);
        this.secondaryTypes = new FactType[sources.length][];
        int nonPlainIndex = 0;
        for (BetaMemoryNode<?> s : sources) {
            FactType[][] grouping = s.getGrouping();
            if (grouping.length > 1) {
                secondaryTypes[nonPlainIndex] = grouping[1];
                sourceMetas[s.getSourceIndex()] = new SourceMeta(s, nonPlainIndex);
                secondary2source[nonPlainIndex] = s.getSourceIndex();
                nonPlainIndex++;
            } else {
                sourceMetas[s.getSourceIndex()] = new SourceMeta(s, -1);
            }
        }
        this.primaryKeyIterators = (ReIterator<KeysStore.Entry>[]) new ReIterator[sourceMetas.length];
        this.secondaryKeyIterators = (ReIterator<KeysStore.Entry>[]) new ReIterator[nonPlainIndex];

        getExpression().setEvaluationState(rule.getRuntime().memory.memoryFactory.getValueResolver(), (factType, fieldIndex) -> evaluationState[factType.getInRuleIndex()].get(fieldIndex));
    }

    private static void resetTransientFlag(FactType[][] grouping, ReIterator<KeysStore.Entry> it, int groupIndex) {
        boolean hasNext = groupIndex < grouping.length - 1;
        while (it.hasNext()) {
            KeysStore.Entry key = it.next();
            ValueRow[] rows = key.key();
            for (ValueRow row : rows) {
                row.setTransient(KeyMode.MAIN.ordinal());
            }
            if (hasNext) {
                resetTransientFlag(grouping, key.getNext().entries(), groupIndex + 1);
            }


        }
    }

    @Override
    public void commitDelta() {
        throw new UnsupportedOperationException();
    }

    void commitDelta1() {
        KeysStore delta = getStore(KeyMode.UNKNOWN_UNKNOWN);
        KeysStore main = getStore(KeyMode.MAIN);


        FactType[][] grouping = getGrouping();
        ReIterator<KeysStore.Entry> it = delta.entries();
        resetTransientFlag(grouping, it, 0);
        main.append(delta);
        delta.clear();
        getStore(KeyMode.KNOWN_UNKNOWN).clear();
    }

    public void computeDelta(boolean deltaOnly) {
        // TODO !!!! remove these checks
        if (getStore(KeyMode.UNKNOWN_UNKNOWN).entries().reset() > 0) throw new IllegalStateException();
        if (getStore(KeyMode.KNOWN_UNKNOWN).entries().reset() > 0) throw new IllegalStateException();


        // This is a two-phase operation
        // 1. We compute delta as a result of MAIN vs NEW KEYS
        // 2. We compute delta as a result of MAIN vs KNOWN KEYS
        KeyMode[][] sourceModes = new KeyMode[getSources().length][2];

        // Zero index always means MAIN data
        for (BetaMemoryNode<?> sourceNode : getSources()) {
            sourceModes[sourceNode.getSourceIndex()][0] = KeyMode.MAIN;
        }

        for (KeyMode deltaMode : KeyMode.DELTA_MODES) {
            for (BetaMemoryNode<?> sourceNode : getSources()) {
                sourceModes[sourceNode.getSourceIndex()][1] = deltaMode;
            }
            computeDelta(sourceModes, getStore(deltaMode), deltaOnly);
        }

        debug();
    }


    private void debug() {
        System.out.println("Node:\t" + this + "\tsources: " + Arrays.toString(getSources()));
        for (KeyMode keyMode : KeyMode.values()) {
            System.out.println("\tMode:\t" + keyMode);
            ReIterator<ValueRow[]> it = iterator(keyMode);
            it.reset();
            int counter = 0;
            while (it.hasNext()) {
                ValueRow[] rows = it.next();
                System.out.println("\t\t" + counter + "\t" + Arrays.toString(rows));
                counter++;
            }
        }
    }


    private void computeDelta(KeyMode[][] sourceModes, KeysStore destination, boolean deltaOnly) {
        computeDelta(sourceModes, destination, deltaOnly, 0, false);
    }

    private void computeDelta(KeyMode[][] sourceModes, KeysStore destination, boolean deltaOnly, int sourceId, boolean hasDelta) {
        boolean lastSource = sourceId == sourceModes.length - 1;
        BetaMemoryNode<?> source = getSources()[sourceId];
        KeyMode[] modes = sourceModes[sourceId];
        for (int i = 0; i < modes.length; i++) {
            boolean newHasDelta = hasDelta || (i != 0); // Remember that zero index always refers to a non-delta source
            KeyMode mode = modes[i];
            ReIterator<KeysStore.Entry> iterator = source.getStore(mode).entries();
            if (lastSource) {
                if ((newHasDelta || (!deltaOnly)) && iterator.reset() > 0) {
                    this.primaryKeyIterators[sourceId] = iterator;
                    this.currentSourceModes[sourceId] = mode;
                    evaluate(destination, 0);
                }
            } else {
                if (iterator.reset() > 0) {
                    this.primaryKeyIterators[sourceId] = iterator;
                    this.currentSourceModes[sourceId] = mode;
                    computeDelta(sourceModes, destination, deltaOnly, sourceId + 1, newHasDelta);
                }
            }
        }
    }

    private void evaluate(KeysStore destination, int sourceIndex) {
        ReIterator<KeysStore.Entry> it = this.primaryKeyIterators[sourceIndex];
        KeyMode mode = this.currentSourceModes[sourceIndex];
        if (it.reset() == 0) return;
        SourceMeta sourceMeta = this.sourceMetas[sourceIndex];
        FactType[] types = sourceMeta.evaluationFacts;
        KeysStore.Entry entry;
        boolean last = sourceIndex == this.primaryKeyIterators.length - 1;

        while (it.hasNext()) {
            entry = it.next();
            setState(entry.key(), types, mode == KeyMode.MAIN);
            if (sourceMeta.secondaryFacts != null) {
                this.secondaryKeyIterators[sourceMeta.nonPlainIndex] = entry.getNext().entries();
            }
            if (last) {
                testAndSaveCurrentState(destination);
            } else {
                evaluate(destination, sourceIndex + 1);
            }
        }
    }

    private void testAndSaveCurrentState(KeysStore destination) {
        if (getExpression().test()) {
            if (secondaryKeyIterators.length == 0) {
                destination.save(saveFunction);
            } else {
                iterateSecondary(0, destination);
            }
        }
    }

    private void iterateSecondary(int index, KeysStore destination) {
        ReIterator<KeysStore.Entry> iterator = secondaryKeyIterators[index];
        if (iterator.reset() == 0) return;
        FactType[] types = secondaryTypes[index];
        KeyMode sourceMode = this.currentSourceModes[secondary2source[index]];
        boolean last = index == secondaryKeyIterators.length - 1;
        while (iterator.hasNext()) {
            ValueRow[] rows = iterator.next().key();
            setState(rows, types, sourceMode == KeyMode.MAIN);
            if (last) {
                destination.save(saveFunction);
            } else {
                iterateSecondary(index + 1, destination);
            }
        }
    }

    private void setState(ValueRow[] rows, FactType[] types, boolean reset) {
        for (int i = 0; i < types.length; i++) {
            ValueRow row = rows[i];
            if (reset) {
                row.setTransient(KeyMode.MAIN.ordinal());
            }
            this.evaluationState[types[i].getInRuleIndex()] = row;
        }
    }

    private static class SourceMeta {
        final BetaMemoryNode<?> source;
        final int nonPlainIndex;
        final FactType[] evaluationFacts;
        final FactType[] secondaryFacts;

        SourceMeta(BetaMemoryNode<?> source, int nonPlainIndex) {
            this.source = source;
            this.nonPlainIndex = nonPlainIndex;
            this.evaluationFacts = source.getGrouping()[0];
            this.secondaryFacts = nonPlainIndex < 0 ? null : source.getGrouping()[1];
        }
    }
}
