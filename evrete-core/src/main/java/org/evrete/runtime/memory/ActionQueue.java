package org.evrete.runtime.memory;

import org.evrete.api.Action;
import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearIdentityHashSet;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

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


    public void fillFrom(ActionQueue<T> other) {
        for (Map.Entry<Action, LinearIdentityHashSet<T>> entry : other.data.entrySet()) {
            this.data.get(entry.getKey()).bulkAdd(entry.getValue());
        }
    }


    public boolean isEmpty() {
        for (LinearIdentityHashSet<T> queue : data.values()) {
            if (queue.size() > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean hasActions(Action... actions) {
        for (Action action : actions) {
            if (data.get(action).size() > 0) {
                return true;
            }
        }
        return false;
    }

    public ReIterator<T> get(Action action) {
        return data.get(action).iterator();
    }

    public void forEach(BiConsumer<? super Action, ? super ReIterable<T>> action) {
        data.forEach(action);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
