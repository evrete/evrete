package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationState;
import org.evrete.runtime.FactType;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class NodeIterationState implements NodeIterationStateFactory.State, BetaEvaluationState {
    private final ValueRow[] stateValues;
    private final IntFunction<IntToValueRow> destinationValues;
    private final boolean nonPlainSources;
    private final BetaEvaluatorGroup evaluator;
    private final IterationStateHandler[] stateHandlers;
    private final IterationStateHandlerNonPlain[] nonPlainStateHandlers;

    public NodeIterationState(BetaConditionNode node) {
        this.evaluator = node.getExpression();
        BetaMemoryNode<?>[] sources = node.getSources();
        this.stateHandlers = new IterationStateHandler[sources.length];

        List<IterationStateHandlerNonPlain> secondaryHandlers = new ArrayList<>(sources.length);
        for (BetaMemoryNode<?> source : sources) {
            FactType[][] grouping = source.getGrouping();
            boolean nonPlain = grouping.length > 1;
            int sourceIndex = source.getSourceIndex();
            if (nonPlain) {
                IterationStateHandlerNonPlain handler = new IterationStateHandlerNonPlain(source);
                this.stateHandlers[sourceIndex] = handler;
                secondaryHandlers.add(handler);
            } else {
                this.stateHandlers[sourceIndex] = new IterationStateHandler(source);
            }
        }
        this.nonPlainStateHandlers = secondaryHandlers.toArray(new IterationStateHandlerNonPlain[0]);

        this.stateValues = new ValueRow[node.getRule().getAllFactTypes().length];

        FactType[][] storageGrouping = node.getGrouping();
        SaveValueAdapter[] saveValues = new SaveValueAdapter[storageGrouping.length];
        for (int level = 0; level < saveValues.length; level++) {
            saveValues[level] = new SaveValueAdapter(storageGrouping[level]);
        }

        this.destinationValues = level -> saveValues[level];
        this.nonPlainSources = nonPlainStateHandlers.length > 0;
    }

    @Override
    public void saveTo(KeysStore destination) {
        destination.save(destinationValues);
    }

    @Override
    public boolean evaluate() {
        return evaluator.test(this);
    }

    @Override
    public Object apply(FactType factType, int fieldIndex) {
        return stateValues[factType.getInRuleIndex()].get(fieldIndex);
    }

    @Override
    public boolean hasNonPlainSources() {
        return nonPlainSources;
    }

    @Override
    public void setEvaluationEntry(KeysStore.Entry entry, int sourceId) {
        stateHandlers[sourceId].process(entry);
    }

    @Override
    public void setSecondaryEntry(KeysStore.Entry entry, int nonPlainIndex) {
        this.nonPlainStateHandlers[nonPlainIndex].setSecondaryState(entry);
    }

    @ThreadUnsafe
    @Override
    public ReIterator<KeysStore.Entry>[] buildSecondary() {
        return nonPlainStateHandlers;
    }

    private class SaveValueAdapter implements IntToValueRow {
        private final FactType[] grouping;

        public SaveValueAdapter(FactType[] grouping) {
            this.grouping = grouping;
        }

        @Override
        public ValueRow apply(int value) {
            return stateValues[grouping[value].getInRuleIndex()];
        }
    }

    private class IterationStateHandler {
        private final FactType[] primary;

        public IterationStateHandler(BetaMemoryNode<?> source) {
            this.primary = source.getGrouping()[0];
        }

        void process(KeysStore.Entry entry) {
            ValueRow[] key = entry.key();
            for (int i = 0; i < primary.length; i++) {
                stateValues[primary[i].getInRuleIndex()] = key[i];
            }
        }
    }

    private class IterationStateHandlerNonPlain extends IterationStateHandler implements ReIterator<KeysStore.Entry> {
        private final FactType[] secondary;
        private ReIterator<KeysStore.Entry> secondaryIterator;

        public IterationStateHandlerNonPlain(BetaMemoryNode<?> source) {
            super(source);
            this.secondary = source.getGrouping()[1];
        }

        @Override
        void process(KeysStore.Entry entry) {
            super.process(entry);
            this.secondaryIterator = entry.getNext().entries();
        }

        @Override
        public long reset() {
            return secondaryIterator.reset();
        }

        @Override
        public boolean hasNext() {
            return secondaryIterator.hasNext();
        }

        void setSecondaryState(KeysStore.Entry entry) {
            ValueRow[] key = entry.key();
            for (int i = 0; i < secondary.length; i++) {
                stateValues[secondary[i].getInRuleIndex()] = key[i];
            }
        }

        @Override
        public KeysStore.Entry next() {
            return secondaryIterator.next();
        }
    }

}
