package org.evrete.runtime.memory;

import org.evrete.api.BufferedInsert;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.collections.FastIdentityHashSet;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class Buffer {
    private static final Logger LOGGER = Logger.getLogger(Buffer.class.getName());
    private final EnumMap<Action, TypedObjects> actionData = new EnumMap<>(Action.class);
    private boolean insertsAvailable = false;
    private boolean deletesAvailable = false;

    public Buffer() {
        for (Action a : Action.values()) {
            actionData.put(a, new TypedObjects());
        }
    }

    public void add(TypeResolver resolver, Action action, Collection<?> objects) {
        int objectCount = objects.size();
        TypedObjects deletes = actionData.get(Action.RETRACT);
        TypedObjects inserts = actionData.get(Action.INSERT);

        switch (action) {
            case INSERT:
                inserts.ensureExtraCapacity(objectCount);
                for (Object o : objects) {
                    insertsAvailable |= inserts.add(resolver, o);
                }
                return;
            case UPDATE:
                inserts.ensureExtraCapacity(objectCount);
                deletes.ensureExtraCapacity(objectCount);
                for (Object o : objects) {
                    insertsAvailable |= inserts.add(resolver, o);
                    deletesAvailable |= deletes.add(resolver, o);
                }
                return;
            case RETRACT:
                deletes.ensureExtraCapacity(objectCount);
                for (Object o : objects) {
                    deletesAvailable |= deletes.add(resolver, o);
                }
                return;
            default:
                throw new IllegalStateException();
        }
    }

    public void insert(TypeResolver resolver, String objectType, Collection<?> objects) {
        int objectCount = objects.size();
        Type<?> type = resolver.getType(objectType);
        if (type == null) {
            LOGGER.warning("Objects of unknown type '" + objectType + "' will be ignored.");
        }

        TypedObjects inserts = actionData.get(Action.INSERT);
        inserts.ensureExtraCapacity(objectCount);
        for (Object o : objects) {
            insertsAvailable |= inserts.add(type, o);
        }
    }


    void takeAll(Action action, BiConsumer<Type<?>, Iterator<Object>> consumer) {
        actionData.get(action).takeAll(consumer);
    }

    public void takeAllFrom(Buffer other) {
        if (!other.hasTasks()) return;
        this.insertsAvailable |= other.insertsAvailable;
        this.deletesAvailable |= other.deletesAvailable;
        other.actionData.forEach(
                (action, otherObjects) -> {
                    TypedObjects myObjects = actionData.get(action);
                    myObjects.add(otherObjects);
                }
        );


        other.clear();
    }


    final boolean hasTasks() {
        return insertsAvailable || deletesAvailable;
    }

    void clear() {
        this.insertsAvailable = false;
        this.deletesAvailable = false;
        for (TypedObjects typedObjects : actionData.values()) {
            typedObjects.clear();
        }
    }

    @Override
    public String toString() {
        return "Buffer{" + actionData + '}';
    }


    private static class TypedObjects implements BufferedInsert {
        private final Map<Type<?>, FastIdentityHashSet<Object>> data = new HashMap<>();

        boolean add(TypeResolver resolver, Object o) {
            Type<?> type = resolver.resolve(o);
            if (type == null) {
                LOGGER.warning("Object {" + o + "} is of an unknown type '" + o.getClass() + "' and will be ignored.");
                return false;
            } else {
                return data.computeIfAbsent(type, t -> new FastIdentityHashSet<>()).add(o);
            }
        }

        boolean add(Type<?> type, Object o) {
            return data.computeIfAbsent(type, t -> new FastIdentityHashSet<>()).add(o);
        }


        void takeAll(BiConsumer<Type<?>, Iterator<Object>> consumer) {
            for (Map.Entry<Type<?>, FastIdentityHashSet<Object>> entry : data.entrySet()) {
                FastIdentityHashSet<Object> set = entry.getValue();
                Type<?> t = entry.getKey();
                consumer.accept(t, set.iterator());
                set.clear();
            }
        }

        @Override
        public void ensureExtraCapacity(int insertCount) {
            for (FastIdentityHashSet<Object> set : data.values()) {
                set.ensureExtraCapacity(insertCount);
            }
        }

        void add(TypedObjects other) {
            for (Map.Entry<Type<?>, FastIdentityHashSet<Object>> entry : other.data.entrySet()) {
                this.data.computeIfAbsent(entry.getKey(), k -> new FastIdentityHashSet<>()).bulkAdd(entry.getValue());
            }
        }

        void clear() {
            for (FastIdentityHashSet<Object> set : data.values()) {
                set.clear();
            }
        }
    }

}
