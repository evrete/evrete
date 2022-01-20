package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;

import java.util.logging.Logger;

public class AtomicMemoryAction {
    private static final Logger LOGGER = Logger.getLogger(AtomicMemoryAction.class.getName());

    public final FactHandle handle;
    public Action action;
    private FactRecordDelta delta;

    AtomicMemoryAction(Action action, FactHandle handle, FactRecordDelta delta) {
        this.action = action;
        this.handle = handle;
        this.delta = delta;
    }

    public FactRecordDelta getDelta() {
        return delta;
    }

    void rebuild(Action newAction, FactRecordDelta newDelta) {

        FactRecordDelta updatedDelta;
        switch (newAction) {
            case INSERT:
                throw new IllegalStateException("Duplicate insert with the same fact handle");
            case UPDATE:
                switch (action) {
                    case RETRACT:
                        // Fact handle has been already deleted, we can't update a deleted entry
                        LOGGER.warning("An attempt was made to update a fact that has been just deleted, update operation skipped");
                        return;
                    case INSERT:
                    case UPDATE:
                        updatedDelta = newDelta;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;
            case RETRACT:
                switch (action) {
                    case RETRACT:
                        // Duplicate delete operation, skipping silently
                        return;
                    case INSERT:
                    case UPDATE:
                        // Deleting a fact that has been just inserted or updated
                        updatedDelta = FactRecordDelta.deleteDelta(this.delta.getLatest());
                        break;
                    default:
                        throw new IllegalStateException();

                }
                break;
            default:
                throw new IllegalStateException();
        }


        this.action = newAction;
        this.delta = updatedDelta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicMemoryAction that = (AtomicMemoryAction) o;
        return handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "{action=" + action +
                ", handle=" + handle +
                ", rec=" + delta +
                '}';
    }
}
