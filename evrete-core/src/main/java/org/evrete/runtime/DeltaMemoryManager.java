package org.evrete.runtime;

import org.evrete.api.Action;

import java.util.Arrays;

class DeltaMemoryManager implements MemoryActionListener {
    private final int[] actionCounts = new int[Action.values().length];
    private int totalActions = 0;

    boolean hasMemoryChanges() {
        return totalActions > 0;
    }

    int deltaOperations() {
        return actionCounts[Action.INSERT.ordinal()] + actionCounts[Action.UPDATE.ordinal()];
    }

    @Override
    public void apply(int type, Action action, int delta) {
        totalActions += delta;
        actionCounts[action.ordinal()] += delta;
    }

    void clear() {
        Arrays.fill(actionCounts, 0);
        totalActions = 0;
    }

    @Override
    public String toString() {
        return "{actions=" + Arrays.toString(actionCounts) +
                ", total=" + totalActions +
                '}';
    }
}
