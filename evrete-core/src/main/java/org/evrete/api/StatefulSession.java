package org.evrete.api;

import org.evrete.runtime.RuntimeRule;

import java.util.Collection;

public interface StatefulSession extends WorkingMemory, RuntimeContext<StatefulSession> {

    void fire();

    void close();

    RuntimeRule getRule(String name);

    default RuntimeRule getRule(Named named) {
        return getRule(named.getName());
    }

    default void insertAndFire(Collection<?> objects) {
        insert(objects);
        fire();
    }

    default void insertAndFire(Object... objects) {
        insert(objects);
        fire();
    }

    default void deleteAndFire(Object... objects) {
        delete(objects);
        fire();
    }

    default void deleteAndFire(Collection<?> objects) {
        delete(objects);
        fire();
    }

}
