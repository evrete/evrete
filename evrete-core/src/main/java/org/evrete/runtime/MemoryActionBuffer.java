package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;
import org.evrete.collections.LinearHashSet;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

public class MemoryActionBuffer {
    private static final Logger LOGGER = Logger.getLogger(MemoryActionBuffer.class.getName());
    private static final BiPredicate<AtomicMemoryAction, FactHandle> SEARCH_FUNCTION = (existing, factHandle) -> existing.handle.equals(factHandle);
    private final LinearHashSet<AtomicMemoryAction> queue;
    private final int type;
    private final MemoryActionListener listener;

    MemoryActionBuffer(int type, int minCapacity, MemoryActionListener listener) {
        this.queue = new LinearHashSet<>(minCapacity);
        this.type = type;
        this.listener = listener;
    }

    AtomicMemoryAction get(FactHandle factHandle) {
        int hash = factHandle.hashCode();
        int binIndex = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
        return queue.get(binIndex);
    }

    void add(Action action, FactHandle factHandle, FactRecord factRecord) {
        queue.resize();
        int hash = factHandle.hashCode();
        int binIndex = queue.findBinIndex(factHandle, hash, SEARCH_FUNCTION);
        AtomicMemoryAction existingAction = queue.get(binIndex);
        if (existingAction == null) {
            queue.saveDirect(new AtomicMemoryAction(action, factHandle, factRecord), binIndex);
            reportAction(action, true);
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
                            // Deleting a fact that has been just inserted
                            existingAction.action = Action.RETRACT;
                            reportAction(Action.INSERT, false);
                            reportAction(Action.RETRACT, true);
                            break;
                        case UPDATE:
                            // Deleting a fact that has been queued for update, updating the action
                            existingAction.action = Action.RETRACT;
                            reportAction(Action.UPDATE, false);
                            reportAction(Action.RETRACT, true);
                            break;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void reportAction(Action action, boolean addOrRemove) {
        listener.onBufferAction(type, action, addOrRemove ? 1 : -1);
    }

    public Iterator<AtomicMemoryAction> actions() {
        return queue.iterator();
    }

    public void clear() {
        this.queue.clear();
    }

    @Override
    public String toString() {
        return "{queue=" + queue +
                '}';
    }
}
