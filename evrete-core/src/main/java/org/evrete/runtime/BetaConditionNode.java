package org.evrete.runtime;

import org.evrete.api.*;

import java.util.Arrays;
import java.util.function.IntFunction;

public class BetaConditionNode extends AbstractBetaConditionNode {
    static final BetaConditionNode[] EMPTY_ARRAY = new BetaConditionNode[0];
    private final ReIterator<KeysStore.Entry>[] secondaryKeyIterators;
    private final ValueRow[] evaluationState;
    private final SourceMeta[] sourceMetas;
    private final IntFunction<IntToValueRow> saveFunction;
    private final FactType[][] secondaryTypes;
    private final int[] secondary2source;
    private boolean mergeToMain = true;


    @SuppressWarnings("unchecked")
    BetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode<?>[] sources) {
        super(rule, descriptor, sources);
        this.evaluationState = new ValueRow[rule.getDescriptor().factTypes.length];
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

    public void setMergeToMain(boolean mergeToMain) {
        this.mergeToMain = mergeToMain;
    }

    @Override
    public void commitDelta() {
        throw new UnsupportedOperationException();
    }

    void commitDelta1() {
        KeysStore delta1 = getStore(KeyMode.UNKNOWN_UNKNOWN);
        KeysStore delta2 = getStore(KeyMode.KNOWN_UNKNOWN);
        if (mergeToMain) {
            KeysStore main = getStore(KeyMode.MAIN);
            FactType[][] grouping = getGrouping();
            ReIterator<KeysStore.Entry> it = delta1.entries();
            resetTransientFlag(grouping, it, 0);
            main.append(delta1);
        }
        delta1.clear();
        delta2.clear();
    }

    public void computeDelta(boolean deltaOnly) {
        computeDelta1(0, false, false, new KeyMode[this.sourceMetas.length], deltaOnly);
        //debug();
    }


    private void computeDelta1(int sourceIndex, boolean hasDelta, boolean hasKnownKeys, KeyMode[] modes, boolean deltaOnly) {
        for (KeyMode mode : KeyMode.values()) {
            boolean newHasDelta = hasDelta || mode.isDeltaMode();
            boolean newHasKnownKeys = hasKnownKeys || (mode == KeyMode.KNOWN_UNKNOWN);
            modes[sourceIndex] = mode;
            if (sourceIndex == sourceMetas.length - 1) {
                if (newHasDelta || (!deltaOnly)) {
                    KeysStore destination = newHasKnownKeys ?
                            getStore(KeyMode.KNOWN_UNKNOWN)
                            :
                            getStore(KeyMode.UNKNOWN_UNKNOWN);

                    // Initializing key sources
                    for (int i = 0; i < sourceMetas.length; i++) {
                        sourceMetas[i].setIterator(modes[i]);
                    }
                    // Evaluate current modes
                    evaluate(destination, 0);
                }
            } else {
                computeDelta1(sourceIndex + 1, newHasDelta, newHasKnownKeys, modes, deltaOnly);
            }

        }
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

    private void evaluate(KeysStore destination, int sourceIndex) {
        SourceMeta meta = this.sourceMetas[sourceIndex];

        ReIterator<KeysStore.Entry> it = meta.currentIterator; //this.primaryKeyIterators[sourceIndex];
        KeyMode mode = meta.currentMode;//this.currentSourceModes[sourceIndex];
        if (it.reset() == 0) return;
        //SourceMeta sourceMeta = this.sourceMetas[sourceIndex];
        FactType[] types = meta.evaluationFacts;
        KeysStore.Entry entry;
        boolean last = sourceIndex == this.sourceMetas.length - 1;

        while (it.hasNext()) {
            entry = it.next();
            setState(entry.key(), types, mode == KeyMode.MAIN);
            if (meta.secondaryFacts != null) {
                this.secondaryKeyIterators[meta.nonPlainIndex] = entry.getNext().entries();
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
        KeyMode sourceMode = this.sourceMetas[secondary2source[index]].currentMode;
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
        ReIterator<KeysStore.Entry> currentIterator;
        KeyMode currentMode;

        SourceMeta(BetaMemoryNode<?> source, int nonPlainIndex) {
            this.source = source;
            this.nonPlainIndex = nonPlainIndex;
            this.evaluationFacts = source.getGrouping()[0];
            this.secondaryFacts = nonPlainIndex < 0 ? null : source.getGrouping()[1];
        }

        void setIterator(KeyMode mode) {
            this.currentMode = mode;
            this.currentIterator = source.getStore(mode).entries();
        }
    }
}
