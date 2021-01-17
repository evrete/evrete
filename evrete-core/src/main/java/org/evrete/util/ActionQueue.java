package org.evrete.util;

import org.evrete.api.Action;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearIdentityHashSet;

import java.util.EnumMap;
import java.util.Objects;

public class ActionQueue<T> {
    private final EnumMap<Action, LinearIdentityHashSet<T>> data = new EnumMap<>(Action.class);

    public ActionQueue() {
        for (Action action : Action.values()) {
            data.put(action, new LinearIdentityHashSet<>());
        }
    }

    public boolean hasData(Action action) {
        return data.get(action).size() > 0;
    }

    public void add(Action action, T o) {
        Objects.requireNonNull(o);
        switch (action) {
            case INSERT:
                data.get(Action.INSERT).addSilent(o);
                data.get(Action.RETRACT).remove(o);
                return;
            case RETRACT:
                data.get(Action.INSERT).remove(o);
                data.get(Action.RETRACT).addSilent(o);
                return;
            case UPDATE:
                data.get(Action.INSERT).addSilent(o);
                data.get(Action.RETRACT).addSilent(o);
                return;
            default:
                throw new IllegalStateException();
        }
    }

    public void clear() {
        for (LinearIdentityHashSet<T> collection : data.values()) {
            collection.clear();
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
