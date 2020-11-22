package org.evrete.runtime;

import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final StatefulSessionImpl session;
    //final EnumMap<Action, Collection<TypeMemory>> changes = new EnumMap<>(Action.class);
    //private static final List<RuntimeRule> EMPTY_AGENDA = new ArrayList<>();
    boolean inserts = false, deletes = false;

    ActivationContext(StatefulSessionImpl session) {
        this.session = session;
        //changes.put(Action.INSERT, new LinkedList<>());
        //changes.put(Action.RETRACT, new LinkedList<>());
    }

    public StatefulSessionImpl getSession() {
        return session;
    }

    public boolean hasInserts() {
        return inserts;
    }

    public boolean hasDeletes() {
        return deletes;
    }

/*
    void doDeletions() {
        ReIterator<TypeMemory> memories = session.typeMemories();
        while (memories.hasNext()) {
            memories.next().performDelete();
        }
    }
*/


/*
    List<RuntimeRule> doInserts() {
        if (inserts) {
            return session.getRuleStorage().propagateInsertChanges(changes.get(Action.INSERT));
        } else {
            return EMPTY_AGENDA;
        }
    }
*/

/*
    boolean update() {
        // Clear previous data
        inserts = false;
        deletes = false;


        ReIterator<TypeMemory> memories = session.typeMemories();
        while (memories.hasNext()) {
            TypeMemory tm = memories.next();
            if (tm.hasMemoryChanges(Action.INSERT)) {
                //changes.get(Action.INSERT).add(tm);
                inserts = true;
            }
            if (tm.hasMemoryChanges(Action.RETRACT)) {
                //changes.get(Action.RETRACT).add(tm);
                deletes = true;
            }
        }

        return inserts || deletes;
    }
*/

    int incrementFireCount() {
        return this.activationCount.getAndIncrement();
    }
}
