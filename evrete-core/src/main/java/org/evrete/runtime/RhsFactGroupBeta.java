package org.evrete.runtime;

import org.evrete.api.*;

import java.util.EnumMap;

public class RhsFactGroupBeta implements RhsFactGroup, KeyReIterators<ValueRow[]> {
    private final RuntimeFactTypeKeyed[] types;
    private final KeyReIterators<ValueRow[]> keyIterators;
    private final ValueRow[][] keyState;
    private final int groupIndex;
    private final RuntimeFact[] iterationState;
    private ValueRow[] currentKey;

    private RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed[] types, KeyReIterators<ValueRow[]> keyIterators, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this.types = types;
        this.keyIterators = keyIterators;
        this.keyState = keyState;
        this.groupIndex = descriptor.getFactGroupIndex();
        this.iterationState = factState[this.groupIndex];
    }

    public RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, BetaEndNode endNode, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this(descriptor, endNode.getEntryNodes(), endNode, keyState, factState);
    }

    public RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed singleType, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this(descriptor, new RuntimeFactTypeKeyed[]{singleType}, singleType.getMappedKeyIterators(), keyState, factState);
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
        ReIterator<RuntimeFact> it = this.currentKey[index].iterator();
        if (index == length - 1) {
            // The last
            while (it.hasNext()) {
                RuntimeFact fact = it.next();
                if (fact.isDeleted()) {
                    // lazy deletion
                    it.remove();
                } else {
                    this.iterationState[index] = fact;
                    r.run();
                }
            }
        } else {
            while (it.hasNext()) {
                RuntimeFact fact = it.next();
                if (fact.isDeleted()) {
                    // lazy deletion
                    it.remove();
                } else {
                    this.iterationState[index] = fact;
                    runForEachFact(index + 1, length, r);
                }
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
