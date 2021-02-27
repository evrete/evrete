package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyMode;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.JoinReIterator;


public class RhsFactGroupBeta extends AbstractRhsFactGroup implements RhsFactGroup {
    private final FactType[] types;
    private final BetaEndNode endNode;
    private final SessionMemory memory;

    RhsFactGroupBeta(SessionMemory memory, RhsFactGroupDescriptor descriptor, BetaEndNode endNode) {
        this.endNode = endNode;
        this.types = descriptor.getTypes();
        this.memory = memory;
    }

    @Override
    public ReIterator<FactHandleVersioned> factIterator(FactType type, ValueRow row) {
        KeyMode mode = KeyMode.values()[row.getTransient()];
        return memory.get(type.getType()).get(type.getFields()).get(type.getAlphaMask()).iterator(mode, row);
    }

    @Override
    public FactType[] types() {
        //TODO !!!! get rid of the RuntimeFact
        return types;
    }

    @Override
    public ReIterator<ValueRow[]> keyIterator(boolean delta) {
        return delta ?
                JoinReIterator.of(endNode.iterator(KeyMode.UNKNOWN_UNKNOWN), endNode.iterator(KeyMode.KNOWN_UNKNOWN))
                :
                endNode.iterator(KeyMode.MAIN);
    }

    @Override
    public String toString() {
        return endNode.toString();
    }

    /*
    static void runCurrentFacts(RhsFactGroupBeta[] groups, Runnable r) {
        runCurrentFacts(0, groups.length - 1, groups, r);
    }
*/

/*
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
*/

/*
    void setKey(ValueRow[] key) {
        this.keyState[groupIndex] = key;
        // TODO !!! optimize by using setIterators if input nodes are all unique
        this.currentKey = key;
    }
*/

/*
    private void runForEachFact(Runnable r) {
        runForEachFact(0, this.currentKey.length, r);
    }
*/

/*
    private void runForEachFact(int index, int length, Runnable r) {
        ReIterator<FactHandleVersioned> it = this.currentKey[index].iterator();
        FactIterationState state = this.state[index];
        if (index == length - 1) {
            // The last
            while (it.hasNext()) {
                if (next(state, it)) {
                    r.run();
                }
            }
        } else {
            while (it.hasNext()) {
                if (next(state, it)) {
                    runForEachFact(index + 1, length, r);
                }
            }
        }
    }
*/

/*
    //@Override
    public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
        return keyIterators.keyIterators();
    }
*/

}
