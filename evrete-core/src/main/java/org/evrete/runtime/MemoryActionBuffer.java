package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;
import org.evrete.collections.LinearHashSet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

class MemoryActionBuffer {
    private static final Logger LOGGER = Logger.getLogger(MemoryActionBuffer.class.getName());
    private static final BiPredicate<AtomicMemoryAction, FactHandle> SEARCH_FUNCTION = (existing, factHandle) -> existing.handle.equals(factHandle);
    private final LinearHashSet<AtomicMemoryAction> queue;
    private final int[] actionCounts = new int[Action.values().length];
    private int totalActions = 0;

    MemoryActionBuffer(int minCapacity) {
        this.queue = new LinearHashSet<>(minCapacity);
    }


    AtomicMemoryAction get(FactHandle factHandle) {
        int hash = factHandle.hashCode();
        int addr = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
        return queue.get(addr);
    }

    synchronized void add(Action action, FactHandle factHandle, LazyInsertState factRecord) {
        queue.resize();
        int hash = factHandle.hashCode();
        int addr = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
        AtomicMemoryAction existingAction = queue.get(addr);
        if (existingAction == null) {
            queue.saveDirect(new AtomicMemoryAction(action, factHandle, factRecord), addr);
            actionCounts[action.ordinal()]++;
            totalActions++;
        } else {
            switch (action) {
                case INSERT:
                    LOGGER.warning("Fact has been already inserted, operation skipped.");
                    break;
                case UPDATE:
                    switch (existingAction.action) {
                        case RETRACT:
                            // Fact handle has been already deleted, we can't update deleted entry
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
                            // Deleting a fact that has been just inserted
                            existingAction.action = Action.RETRACT;
                            actionCounts[Action.INSERT.ordinal()]--;
                            actionCounts[Action.RETRACT.ordinal()]++;
                            break;
                        case UPDATE:
                            // Deleting a fact that has been queued for update, updating the action
                            existingAction.action = Action.RETRACT;
                            actionCounts[Action.UPDATE.ordinal()]--;
                            actionCounts[Action.RETRACT.ordinal()]++;
                            break;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }


    boolean hasData() {
        return totalActions > 0;
    }

    Iterator<AtomicMemoryAction> actions() {
        return queue.iterator();
    }

    int deltaOperations() {
        return actionCounts[Action.INSERT.ordinal()] + actionCounts[Action.UPDATE.ordinal()];
    }

    void clear() {
        Arrays.fill(actionCounts, 0);
        totalActions = 0;
        this.queue.clear();
    }

    @Override
    public String toString() {
        return "{queue=" + queue +
                ", actions=" + Arrays.toString(actionCounts) +
                ", total=" + totalActions +
                '}';
    }
}
