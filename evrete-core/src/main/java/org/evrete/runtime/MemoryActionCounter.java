package org.evrete.runtime;

import org.evrete.api.Action;

import java.util.Arrays;

class MemoryActionCounter implements MemoryActionListener {
    private final int[] actionCounts = new int[Action.values().length];
    private int totalActions = 0;

    boolean hasData() {
        return totalActions > 0;
    }

    int deltaOperations() {
        return actionCounts[Action.INSERT.ordinal()] + actionCounts[Action.UPDATE.ordinal()];
    }

    @Override
    public void apply(Action action, boolean flag) {
        int delta = flag ? 1 : -1;
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
