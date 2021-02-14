/*
package org.evrete.util;

import org.evrete.api.*;
import org.evrete.collections.LinearHashSet;
import org.evrete.runtime.SessionMemory;

import java.util.EnumMap;
import java.util.Objects;
import java.util.logging.Logger;

public class ActionQueue {
    private static final Logger LOGGER = Logger.getLogger(ActionQueue.class.getName());
    private final EnumMap<Action, LinearHashSet<FactEntryTypedImpl>> data = new EnumMap<>(Action.class);
    public static final Action[] BUFFER_PROCESSING_SEQUENCE = new Action[]{
            Action.RETRACT,
            Action.UPDATE,
            Action.INSERT
    };

    public ActionQueue() {
        for (Action action : Action.values()) {
            data.put(action, new LinearHashSet<>());
        }
    }

    public boolean hasData(Action action) {
        return data.get(action).size() > 0;
    }

    public void add(Action action, FactHandle factHandle, Object instance) {
*/
/*
        if(instance == null) {
            LOGGER.warning("Object not found for fact handle " + factHandle + ", " + action + " operation skipped");
            return false;
        }

        if(factHandle == null) {
            LOGGER.warning("Fact handle not found for object " + instance + ", " + action + " operation skipped");
            return false;
        }
*//*


        add(action, new FactEntryTypedImpl(factHandle, instance));
        //return true;
    }

    private void add(Action action, FactEntryTypedImpl o) {
        data.get(action).addSilent(o);
*/
/*
        switch (action) {
            case INSERT:
                data.get(Action.INSERT).addSilent(o);
                //data.get(Action.RETRACT).remove(o);
                return;
            case RETRACT:
                //data.get(Action.INSERT).remove(o);
                data.get(Action.RETRACT).addSilent(o);
                return;
            case UPDATE:
                //data.get(Action.INSERT).addSilent(o);
                data.get(Action.UPDATE).addSilent(o);
                return;
            default:
                throw new IllegalStateException();
        }
*//*

    }

    public void clear() {
        for (LinearHashSet<FactEntryTypedImpl> collection : data.values()) {
            collection.clear();
        }
    }

    public void clear(Action action) {
        data.get(action).clear();
    }

    public ReIterator<FactEntry> get(Action action) {
        //TODO !!! make static
        return data.get(action).iterator(tuple -> tuple);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    private static class FactEntryTypedImpl implements FactEntry {
        final FactHandle factHandle;
        final Object instance;

        FactEntryTypedImpl(FactHandle factHandle, Object instance) {
            this.factHandle = factHandle;
            this.instance = instance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FactEntryTypedImpl that = (FactEntryTypedImpl) o;
            return factHandle.equals(that.factHandle);
        }

        @Override
        public int hashCode() {
            return factHandle.hashCode();
        }

        @Override
        public FactHandle getHandle() {
            return factHandle;
        }

        @Override
        public Object getFact() {
            return instance;
        }
    }
}
*/
