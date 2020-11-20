package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.memory.TypeMemory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final StatefulSessionImpl session;
    final EnumMap<Action, Collection<TypeMemory>> changes = new EnumMap<>(Action.class);
    private static final List<RuntimeRule> EMPTY_AGENDA = new ArrayList<>();

    ActivationContext(StatefulSessionImpl session) {
        this.session = session;
        changes.put(Action.INSERT, new LinkedList<>());
        changes.put(Action.RETRACT, new LinkedList<>());
    }

    public StatefulSessionImpl getSession() {
        return session;
    }


    void doDeletions() {
        Collection<TypeMemory> memoriesDelete = changes.get(Action.RETRACT);

        if (memoriesDelete.size() > 0) {
            // Clear the memories themselves
            for (TypeMemory tm : memoriesDelete) {
                tm.performDelete();
            }
        }
    }


    List<RuntimeRule> doInserts() {
        Collection<TypeMemory> memoriesInsert = changes.get(Action.INSERT);
        if (memoriesInsert.isEmpty()) {
            return EMPTY_AGENDA;
        } else {
            return session.getRuleStorage().propagateInsertChanges(memoriesInsert);
        }
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
