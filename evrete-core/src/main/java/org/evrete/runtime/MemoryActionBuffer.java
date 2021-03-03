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

    synchronized void add(Action action, FactHandle factHandle, LazyInsertState factRecord) {
        queue.resize();
        int hash = factHandle.hashCode();
        int addr = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);

        AtomicMemoryAction actionSameHandle = queue.get(addr);
        if (actionSameHandle == null) {
            queue.saveDirect(new AtomicMemoryAction(action, factHandle, factRecord), addr);
            actionCounts[action.ordinal()]++;
            totalActions++;
        } else {
            switch (action) {
                case INSERT:
                    throw new IllegalStateException("FactHandle exists");
                case UPDATE:
                    switch (actionSameHandle.action) {
                        case RETRACT:
                            // Fact handle has been already deleted, we can't update deleted entry
                            LOGGER.warning("An attempt was made to update a fact that has been just deleted, update operation skipped");
                            break;
                        case INSERT:
                            // Fact handle has been just inserted, issuing a warning
                            LOGGER.warning("An attempt was made to update a fact that has been just inserted, update operation skipped");
                            break;
                        case UPDATE:
                            // Duplicate update operation, updating the instance
                            actionSameHandle.factRecord = factRecord;
                            break;
                    }
                case RETRACT:
                    switch (actionSameHandle.action) {
                        case RETRACT:
                            // Duplicate delete operation, skipping silently
                            break;
                        case INSERT:
                            // Deleting a fact that has been just inserted
                            LOGGER.warning("Deleting a fact that has been just inserted, both operation cancelled as if they both never happened.");
                            queue.markDeleted(addr);
                            actionCounts[Action.INSERT.ordinal()]--;
                            totalActions--;
                            break;
                        case UPDATE:
                            // Deleting a fact that has been queued for update, updating the action
                            actionSameHandle.action = Action.RETRACT;
                            actionCounts[Action.UPDATE.ordinal()]--;
                            actionCounts[Action.RETRACT.ordinal()]++;
                            break;
                    }
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
