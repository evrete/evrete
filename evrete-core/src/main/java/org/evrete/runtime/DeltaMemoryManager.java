package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Arrays;

class DeltaMemoryManager implements MemoryActionListener {
    private final int[] actionCounts = new int[Action.values().length];
    private int totalActions = 0;
    private final Mask<MemoryAddress> insertDeltaMask = Mask.addressMask();
    private final Mask<MemoryAddress> deleteBufferMask = Mask.addressMask();

    boolean hasMemoryChanges() {
        return totalActions > 0;
    }

    int deltaOperations() {
        return actionCounts[Action.INSERT.ordinal()] + actionCounts[Action.UPDATE.ordinal()];
    }

    @Override
    public void onBufferAction(int type, Action action, int delta) {
        totalActions += delta;
        actionCounts[action.ordinal()] += delta;
    }

    //TODO !!!! no synchronized!!
    synchronized void onInsert(MemoryAddress address) {
        insertDeltaMask.set(address);
    }

    //TODO !!!! no synchronized!!
    synchronized void onDelete(Mask<MemoryAddress> mask) {
        deleteBufferMask.or(mask);
    }

    void clearBufferData() {
        Arrays.fill(actionCounts, 0);
        totalActions = 0;
    }

    Mask<MemoryAddress> getInsertDeltaMask() {
        return insertDeltaMask;
    }

    void clearDeltaData() {
        this.insertDeltaMask.clear();
    }

    Mask<MemoryAddress> getDeleteDeltaMask() {
        return deleteBufferMask;
    }

    void clearDeleteData() {
        this.deleteBufferMask.clear();
    }

    @Override
    public String toString() {
        return "{actions=" + Arrays.toString(actionCounts) +
                ", total=" + totalActions +
                '}';
    }
}
