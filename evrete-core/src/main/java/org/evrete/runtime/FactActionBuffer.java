package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.Type;
import org.evrete.collections.LinearHashSet;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class FactActionBuffer {
    private final Map<Integer, ActionQueue> typedQueues = new ConcurrentHashMap<>();
    private final int capacity;
    private long totalActions = 0L;

    FactActionBuffer(int capacity) {
        this.capacity = capacity;
    }

    boolean hasData() {
        return totalActions > 0;
    }

    private void add(Action action, FactHandle factHandle, FactRecordDelta delta) {
        if (get(factHandle).add(action, factHandle, Objects.requireNonNull(delta))) {
            this.totalActions++;
        }
    }

    AtomicMemoryAction find(FactHandle factHandle) {
        return get(factHandle).get(factHandle);
    }

    void clear() {
        this.typedQueues.values().forEach(ActionQueue::clear);
        this.totalActions = 0L;
    }

    private ActionQueue get(Type<?> t) {
        return get(t.getId());
    }

    private ActionQueue get(FactHandle h) {
        return get(h.getTypeId());
    }

    private ActionQueue get(int typeId) {
        return typedQueues.computeIfAbsent(typeId, i -> new ActionQueue(capacity));
    }

    void copyToAndClear(FactActionBuffer other) {
        this.typedQueues.values().forEach(queue -> queue.queue.forEachDataEntry(a -> other.add(a.action, a.handle, a.getDelta())));
        this.clear();
    }

    void newUpdate(FactHandle handle, FactRecord previous, Object updatedFact) {
        this.add(Action.UPDATE, handle, FactRecordDelta.updateDelta(previous, updatedFact));
    }

    void newInsert(FactHandle handle, FactRecord record) {
        this.add(Action.INSERT, handle, FactRecordDelta.insertDelta(record));
    }

    public void forEach(Consumer<AtomicMemoryAction> consumer) {
        this.typedQueues.values().forEach(q -> q.forEachDataEntry(consumer));
    }

    public ReIterator<AtomicMemoryAction> actions(Type<?> type) {
        return get(type).queue.iterator();
    }

    public void forEach(Type<?> t, Consumer<AtomicMemoryAction> consumer) {
        get(t).forEachDataEntry(consumer);
    }

    void newDelete(FactHandle handle, FactRecord record) {
        this.add(Action.RETRACT, handle, FactRecordDelta.deleteDelta(record));
    }


    private static class ActionQueue {
        private static final BiPredicate<AtomicMemoryAction, FactHandle> SEARCH_FUNCTION = (existing, factHandle) -> existing.handle.equals(factHandle);
        private final LinearHashSet<AtomicMemoryAction> queue;

        ActionQueue(int capacity) {
            this.queue = new LinearHashSet<>(capacity);
        }

        AtomicMemoryAction get(FactHandle factHandle) {
            return queue.get(factHandle, SEARCH_FUNCTION);
        }

        void forEachDataEntry(Consumer<AtomicMemoryAction> consumer) {
            queue.forEachDataEntry(consumer);
        }

        boolean add(Action action, FactHandle factHandle, FactRecordDelta delta) {
            return queue.computeIfAbsent(
                    factHandle,
                    SEARCH_FUNCTION,
                    handle -> new AtomicMemoryAction(action, handle, delta),
                    existingAction -> existingAction.rebuild(action, delta)
            );
        }

        void clear() {
            this.queue.clear();
        }

    }
}
