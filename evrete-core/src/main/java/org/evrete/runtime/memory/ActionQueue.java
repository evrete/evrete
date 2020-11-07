package org.evrete.runtime.memory;

import org.evrete.api.Action;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;

public class ActionQueue<T> {
    private final EnumMap<Action, Collection<T>> data = new EnumMap<>(Action.class);

    public ActionQueue() {
        for (Action action : Action.values()) {
            data.put(action, new LinkedList<>());
        }
    }

    public void add(Action action, T o) {
        if (o != null) {
            data.get(action).add(o);
        }
    }

    public void clear() {
        for (Collection<T> queue : data.values()) {
            queue.clear();
        }
    }


    public boolean isEmpty() {
        for (Collection<T> queue : data.values()) {
            if (queue.size() > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean hasActions(Action... actions) {
        for (Action action : actions) {
            if (!data.get(action).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Collection<T> get(Action action) {
        return data.get(action);
    }

    public void forEach(BiConsumer<? super Action, ? super Collection<T>> action) {
        data.forEach(action);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
