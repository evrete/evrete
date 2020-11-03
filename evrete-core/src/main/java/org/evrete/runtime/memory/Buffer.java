package org.evrete.runtime.memory;

import org.evrete.api.Action;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class Buffer {
    private static final Logger LOGGER = Logger.getLogger(Buffer.class.getName());
    private final Map<Type<?>, ActionQueue<Object>> actionData = new HashMap<>();
    private final int[] inserts;

    public Buffer() {
        this.inserts = new int[Action.values().length];
    }

    public void add(TypeResolver resolver, Action action, Collection<?> objects) {
        for (Object o : objects) {
            addSingle(resolver, action, o);
        }
    }

    public void addSingle(TypeResolver resolver, Action action, Object o) {
        Type<?> t = resolver.resolve(o);
        addSingle(t, action, o);
    }

    public void addSingle(Type<?> t, Action action, Object o) {
        Objects.requireNonNull(o);
        if (t == null) {
            LOGGER.warning("Object {" + o + "} is of an unknown type '" + o.getClass() + "' and will be ignored.");
        } else {
            if (action == Action.UPDATE) {
                addSingle(t, Action.RETRACT, o);
                addSingle(t, Action.INSERT, o);
            }
            this.actionData.computeIfAbsent(t, k -> new ActionQueue<>()).add(action, o);
            this.inserts[action.ordinal()]++;
        }
    }

    public void insert(TypeResolver resolver, String objectType, Collection<?> objects) {
        Type<?> type = resolver.getType(objectType);
        if (type == null) {
            LOGGER.warning("Objects of unknown type '" + objectType + "' will be ignored.");
        } else {
            for (Object o : objects) {
                addSingle(type, Action.INSERT, o);
            }
        }
    }

    void forEach(Action action, BiConsumer<Type<?>, Queue<Object>> consumer) {
        for (Map.Entry<Type<?>, ActionQueue<Object>> entry : actionData.entrySet()) {
            Type<?> t = entry.getKey();
            Queue<Object> queue = entry.getValue().get(action);
            consumer.accept(t, queue);
        }
    }

    public void takeAllFrom(Buffer other) {
        for (Map.Entry<Type<?>, ActionQueue<Object>> entry : other.actionData.entrySet()) {
            Type<?> t = entry.getKey();
            ActionQueue<Object> otherQueue = entry.getValue();
            otherQueue.forEach((action, objects) -> {
                for (Object o : objects) {
                    addSingle(t, action, o);
                }
            });
        }
        other.clear();
    }


    public final boolean hasData(Action... actions) {
        for (Action action : actions) {
            if (inserts[action.ordinal()] > 0) return true;
        }
        return false;
    }

    public void clear() {
        this.actionData.clear();
        Arrays.fill(this.inserts, 0);
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "actionData=" + actionData +
                ", inserts=" + Arrays.toString(inserts) +
                '}';
    }
}
