package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.ReIterator;
import org.evrete.runtime.memory.TypeMemory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final StatefulSessionImpl session;
    final EnumMap<Action, Collection<TypeMemory>> changes = new EnumMap<>(Action.class);

    ActivationContext(StatefulSessionImpl session) {
        this.session = session;
        changes.put(Action.INSERT, new LinkedList<>());
        changes.put(Action.RETRACT, new LinkedList<>());
    }

    public StatefulSessionImpl getSession() {
        return session;
    }

    boolean update() {
        // Clear previous data
        for (Collection<TypeMemory> collection : changes.values()) {
            collection.clear();
        }


        boolean inserts = false, deletes = false;
        ReIterator<TypeMemory> memories = session.typeMemories();
        while (memories.hasNext()) {
            TypeMemory tm = memories.next();
            if (tm.hasMemoryChanges(Action.INSERT)) {
                changes.get(Action.INSERT).add(tm);
                inserts = true;
            }
            if (tm.hasMemoryChanges(Action.RETRACT)) {
                changes.get(Action.RETRACT).add(tm);
                deletes = true;
            }
        }

        return inserts || deletes;
    }

    int incrementFireCount() {
        return this.activationCount.getAndIncrement();
    }
}
