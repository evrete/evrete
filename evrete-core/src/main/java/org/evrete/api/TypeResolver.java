package org.evrete.api;

import java.util.Collection;

public interface TypeResolver extends Copyable<TypeResolver> {

    Type getType(String name);

    Collection<Type> getKnownTypes();

    Type declare(String typeName);

    default Type getOrDeclare(String typeName) {
        Type t = getType(typeName);
        if (t == null) {
            t = declare(typeName);
        }
        return t;
    }

    default Type getOrDeclare(Class<?> cl) {
        return getOrDeclare(cl.getName());
    }

    default Type declare(Class<?> type) {
        return declare(type.getName());
    }

    Type resolve(Object o);

}
