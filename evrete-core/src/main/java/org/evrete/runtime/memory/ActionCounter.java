package org.evrete.runtime.memory;

import org.evrete.api.Action;

public class ActionCounter {
    private final int[] counts = new int[Action.values().length];

    public ActionCounter() {
        reset();
    }

    public void increment(Action action) {
        this.counts[action.ordinal()]++;
    }

    public boolean hasActions(Action... actions) {
        for (Action a : actions) {
            if (this.counts[a.ordinal()] > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAction(Action action) {
        return this.counts[action.ordinal()] > 0;
    }

    void reset() {
        for (Action action : Action.values()) {
            reset(action);
        }
    }


    void reset(Action action) {
        this.counts[action.ordinal()] = 0;
    }
}
