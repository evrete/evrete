package org.evrete.helper;

import org.evrete.api.StatefulSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public interface SessionWrapper {

    static SessionWrapper of(KieSession session) {
        return new SessionWrapper() {
            @Override
            public void insert(Object o) {
                session.insert(o);
            }

            @Override
            public void fire() {
                session.fireAllRules();
            }

            @Override
            public void close() {
                session.dispose();
            }

/*
            @Override
            public void retractAll() {
                Collection<FactHandle> col = session.getFactHandles();
                if (col == null) return;
                for (FactHandle fh : col) {
                    session.delete(fh);
                }
                session.fireAllRules();
                //session.dispose();
            }
*/

            @Override
            public Collection<Object> getMemoryObjects() {
                Collection<FactHandle> handles = session.getFactHandles();
                Collection<Object> collection = new ArrayList<>(handles.size());
                for (FactHandle h : handles) {
                    collection.add(session.getObject(h));
                }
                return collection;
            }
        };
    }

    static SessionWrapper of(StatefulSession session) {
        return new SessionWrapper() {
            @Override
            public void insert(Object o) {
                session.insert(o);
            }

            @Override
            public void fire() {
                session.fire();
            }

            @Override
            public void close() {
                session.close();
            }

/*
            @Override
            public void retractAll() {
                session.forEachFact((handle, o) -> session.delete(handle));
                session.fire();
            }
*/

            @Override
            public Collection<Object> getMemoryObjects() {
                Collection<FactEntry> entries = TestUtils.sessionObjects(session);

                Collection<Object> collection = new LinkedList<>();
                entries.forEach(factEntry -> collection.add(factEntry.getFact()));
                return collection;
            }
        };
    }

    void insert(Object o);

    default void insertAndFire(Object... objects) {
        for (Object o : objects) {
            insert(o);
        }
        fire();
    }

    void fire();

    void close();

    //void retractAll();

    Collection<Object> getMemoryObjects();
}
