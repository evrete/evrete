package org.evrete.runtime.memory;

import org.evrete.api.Action;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearIdentityHashSet;

import java.util.EnumMap;

public class ActionQueue<T> {
    private final EnumMap<Action, LinearIdentityHashSet<T>> data = new EnumMap<>(Action.class);

    public ActionQueue() {
        for (Action action : Action.values()) {
            data.put(action, new LinearIdentityHashSet<>());
        }
    }

    public void add(Action action, T o) {
        if (o != null) {
            data.get(action).add(o);
        }
    }

    public void clear() {
        for (Action action : Action.values()) {
            clear(action);
        }
    }

    public void clear(Action action) {
        data.get(action).clear();
    }

    public ReIterator<T> get(Action action) {
        return data.get(action).iterator();
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
