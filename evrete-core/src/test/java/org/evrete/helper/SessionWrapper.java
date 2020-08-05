package org.evrete.helper;

import org.evrete.api.StatefulSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.Collection;

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

            @Override
            public void retractAll() {
                Collection<FactHandle> col = session.getFactHandles();
                if (col == null) return;
                for (FactHandle fh : col) {
                    session.delete(fh);
                }
                session.fireAllRules();
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

            @Override
            public void retractAll() {
                session.forEachMemoryObject(session::delete);
                session.fire();
                //session.clear();
            }
        };
    }

    void insert(Object o);

    void fire();

    void close();

    void retractAll();
}
