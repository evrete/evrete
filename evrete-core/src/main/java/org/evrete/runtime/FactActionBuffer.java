package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.Type;
import org.evrete.collections.LinearHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class FactActionBuffer {
    private static final Logger LOGGER = Logger.getLogger(FactActionBuffer.class.getName());

    private final Map<Integer, ActionQueue> typedQueues = new ConcurrentHashMap<>();

    private final int[] actionCounts = new int[Action.values().length];
    private long totalActions = 0L;
    private final int capacity;

    FactActionBuffer(int capacity) {
        this.capacity = capacity;
    }

    boolean hasData() {
        return totalActions > 0;
    }

    private void add(Action action, FactHandle factHandle, FactRecord factRecord) {
        if(get(factHandle).add(action, factHandle, factRecord)) {
            this.actionCounts[action.ordinal()] ++;
            this.totalActions++;
        }
    }

    AtomicMemoryAction find(FactHandle factHandle) {
        return get(factHandle).get(factHandle);
    }

    void clear() {
        Arrays.fill(this.actionCounts, 0);
        this.typedQueues.values().forEach(ActionQueue::clear);
        this.totalActions = 0L;
    }

    int deltaOperations() {
        return getCount(Action.INSERT, Action.UPDATE);
    }

    private ActionQueue get(Type<?> t) {
        return get(t.getId());
    }

    private ActionQueue get(FactHandle h) {
        return get(h.getTypeId());
    }
    private ActionQueue get(int typeId) {
        return typedQueues.computeIfAbsent(typeId, i->new ActionQueue(capacity));
    }

    void copyToAndClear(FactActionBuffer other) {

        this.typedQueues.values().forEach(queue -> queue.queue.forEachDataEntry(a -> other.add(a.action, a.handle, a.factRecord)));
        this.clear();
    }

    private int getCount(Action... actions) {
        int ret = 0;
        for(Action a : actions) {
            ret+= actionCounts[a.ordinal()];
        }
        return ret;
    }

    void newUpdate(FactHandle handle, Object object) {
        this.add(Action.UPDATE, handle, new FactRecord(object));
    }

    void newInsert(FactHandle handle, FactRecord record) {
        this.add(Action.INSERT, handle, record);
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

    void newDelete(FactHandle handle) {
        this.add(Action.RETRACT, handle, null);
    }


    private static class ActionQueue {
        private final LinearHashSet<AtomicMemoryAction> queue;
        private static final BiPredicate<AtomicMemoryAction, FactHandle> SEARCH_FUNCTION = (existing, factHandle) -> existing.handle.equals(factHandle);

        ActionQueue(int capacity) {
            this.queue = new LinearHashSet<>(capacity);
        }

        AtomicMemoryAction get(FactHandle factHandle) {
            int hash = factHandle.hashCode();
            int binIndex = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
            return queue.get(binIndex);
        }

        void forEachDataEntry(Consumer<AtomicMemoryAction> consumer) {
            queue.forEachDataEntry(consumer);
        }

        boolean add(Action action, FactHandle factHandle, FactRecord factRecord) {
            queue.resize();
            int hash = factHandle.hashCode();
            int binIndex = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
            AtomicMemoryAction existingAction = queue.get(binIndex);
            if (existingAction == null) {
                queue.saveDirect(new AtomicMemoryAction(action, factHandle, factRecord), binIndex);
                return true;
            } else {
                switch (action) {
                    case INSERT:
                        LOGGER.warning("Fact has been already inserted, operation skipped.");
                        break;
                    case UPDATE:
                        switch (existingAction.action) {
                            case RETRACT:
                                // Fact handle has been already deleted, we can't update a deleted entry
                                LOGGER.warning("An attempt was made to update a fact that has been just deleted, update operation skipped");
                                break;
                            case INSERT:
                            case UPDATE:
                                existingAction.factRecord = factRecord;
                                break;
                        }
                        break;
                    case RETRACT:
                        switch (existingAction.action) {
                            case RETRACT:
                                // Duplicate delete operation, skipping silently
                                break;
                            case INSERT:
                            case UPDATE:
                                // Deleting a fact that has been just inserted or updated
                                existingAction.action = Action.RETRACT;
                                break;
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
                return false;
            }
        }

        public void clear() {
            this.queue.clear();
        }

    }

}
