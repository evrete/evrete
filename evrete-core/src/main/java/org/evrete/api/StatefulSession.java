package org.evrete.api;

import java.util.Collection;
import java.util.function.BooleanSupplier;

public interface StatefulSession extends RuleSession<StatefulSession> {

    StatefulSession setFireCriteria(BooleanSupplier fireCriteria);

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

}
