package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.NestedReIterator;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class RhsFactGroupBeta implements RhsFactGroup, KeyReIterators<ValueRow[]> {
    private final RuntimeFactTypeKeyed[] types;
    private final KeyReIterators<ValueRow[]> keyIterators;
    private final ValueRow[][] keyState;
    private final int groupIndex;
    private final NestedReIterator<RuntimeFact> nestedFactIterator;

    private RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed[] types, KeyReIterators<ValueRow[]> keyIterators, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this.types = types;
        this.keyIterators = keyIterators;
        this.keyState = keyState;
        this.groupIndex = descriptor.getFactGroupIndex();
        this.nestedFactIterator = new NestedReIterator<RuntimeFact>(factState[groupIndex]) {
            @Override
            protected void set(int index, RuntimeFact obj) {
                super.set(index, obj);
            }
        };
    }

    public RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, BetaEndNode endNode, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this(descriptor, endNode.getEntryNodes(), endNode, keyState, factState);
    }

    public RhsFactGroupBeta(RhsFactGroupDescriptor descriptor, RuntimeFactTypeKeyed singleType, ValueRow[][] keyState, RuntimeFact[][] factState) {
        this(descriptor, new RuntimeFactTypeKeyed[]{singleType}, singleType.getMappedKeyIterators(), keyState, factState);
    }

    static void runKeys(ScanMode mode, RhsFactGroupBeta[] groups, Runnable r) {
        switch (mode) {
            case DELTA:
                runDelta(0, groups.length - 1, false, groups, r);
                return;
            case KNOWN:
                runKnown(0, groups.length - 1, groups, r);
                return;
            case FULL:
                runFull(0, groups.length - 1, groups, r);
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void runDelta(int index, int lastIndex, boolean hasDelta, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        KeyMode mode;
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                if ((mode.isDeltaMode() || hasDelta) && iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            r.run();
                        }
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            runDelta(index + 1, lastIndex, mode.isDeltaMode(), groups, r);
                        }
                    }
                }
            }
        }
    }

    private static void runFull(int index, int lastIndex, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            r.run();
                        }
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            runFull(index + 1, lastIndex, groups, r);
                        }
                    }
                }
            }
        }
    }

    private static void runKnown(int index, int lastIndex, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        KeyMode mode;
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                //TODO !!!! optimize it, there's only one non-delta iterator!!!
                if ((!mode.isDeltaMode()) && iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            r.run();
                        }
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                //TODO !!!! optimize it, there's only one non-delta iterator!!!
                if (iterator.reset() > 0 && (!mode.isDeltaMode())) {
                    while (iterator.hasNext()) {
                        if (group.setKey(iterator.next())) {
                            runKnown(index + 1, lastIndex, groups, r);
                        }
                    }
                }
            }
        }
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

    private boolean setKey(ValueRow[] key) {
        this.keyState[groupIndex] = key;

        //System.out.println(keyIterators + ": " + groupIndex + " = " + Arrays.toString(key));
        // TODO !!! optimize by using setIterators if input nodes are all unique
        this.nestedFactIterator.setIterables(key);
        return true;
    }

    private void runForEachFact(Runnable r) {
        nestedFactIterator.runForEach(r);
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

    @Override
    public boolean isInActiveState() {
        return readState(types);
    }

    @Override
    public void resetState() {
        resetState(types);
    }
}
