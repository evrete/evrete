package org.evrete.runtime;

import org.evrete.api.*;

import java.util.EnumMap;


public class RhsFactGroupBeta extends AbstractRhsFactGroup implements RhsFactGroup, KeyReIterators<ValueRow[]> {
    private final RuntimeFactTypeKeyed[] types;
    private final KeyReIterators<ValueRow[]> keyIterators;
    private final ValueRow[][] keyState;
    private final int groupIndex;
    private ValueRow[] currentKey;

    private RhsFactGroupBeta(SessionMemory runtime, RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed[] types, KeyReIterators<ValueRow[]> keyIterators, ValueRow[][] keyState, FactIterationState[][] factState) {
        super(runtime, factState[descriptor.getFactGroupIndex()]);
        this.types = types;
        this.keyIterators = keyIterators;
        this.keyState = keyState;
        this.groupIndex = descriptor.getFactGroupIndex();
    }

    RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, BetaEndNode endNode, ValueRow[][] keyState, FactIterationState[][] factState) {
        this(endNode.getRuntime().getMemory(), descriptor, endNode.getEntryNodes(), endNode, keyState, factState);
    }

    RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed singleType, ValueRow[][] keyState, FactIterationState[][] factState) {
        this(singleType.getRuntime().getMemory(), descriptor, new RuntimeFactTypeKeyed[]{singleType}, singleType.getMappedKeyIterators(), keyState, factState);
    }

    static void runCurrentFacts(RhsFactGroupBeta[] groups, Runnable r) {
        runCurrentFacts(0, groups.length - 1, groups, r);
    }

    private static void runCurrentFacts(int index, int lastIndex, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];

        if (index == lastIndex) {
            group.runForEachFact(r);
        } else {
            int nextIndex = index + 1;
            Runnable nested = () -> runCurrentFacts(nextIndex, lastIndex, groups, r);
            group.runForEachFact(nested);
        }
    }

    void setKey(ValueRow[] key) {
        this.keyState[groupIndex] = key;
        // TODO !!! optimize by using setIterators if input nodes are all unique
        this.currentKey = key;
    }

    private void runForEachFact(Runnable r) {
        runForEachFact(0, this.currentKey.length, r);
    }

    private void runForEachFact(int index, int length, Runnable r) {
        ReIterator<FactHandleVersioned> it = this.currentKey[index].iterator();
        FactIterationState state = this.state[index];
        if (index == length - 1) {
            // The last
            while (it.hasNext()) {
                if (next(state, it)) {
                    r.run();
                }
/*
                FactHandle fact = it.next();
                if (fact.isDeleted()) {
                    // lazy deletion
                    it.remove();
                } else {
                    this.iterationState[index] = fact;
                    r.run();
                }
*/
            }
        } else {
            while (it.hasNext()) {
                if (next(state, it)) {
                    runForEachFact(index + 1, length, r);
                }
/*
                FactHandle fact = it.next();
                if (fact.isDeleted()) {
                    // lazy deletion
                    it.remove();
                } else {
                    this.iterationState[index] = fact;
                    runForEachFact(index + 1, length, r);
                }
*/
            }
        }
    }

    @Override
    public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
        return keyIterators.keyIterators();
    }

    @Override
    public boolean isAlpha() {
        return false;
    }

    @Override
    public int getIndex() {
        return groupIndex;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RuntimeFactTypeKeyed[] getTypes() {
        return types;
    }
}
