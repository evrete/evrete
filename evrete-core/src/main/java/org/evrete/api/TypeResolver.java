package org.evrete.api;

import java.util.Collection;

public interface TypeResolver extends Copyable<TypeResolver> {

    <T> Type<T> getType(String name);

    Collection<Type<?>> getKnownTypes();

    <T> Type<T> declare(String typeName, Class<T> javaType);

    <T> Type<T> declare(String typeName, String javaType);

    default <T> Type<T> getOrDeclare(String typeName, Class<T> javaType) {
        Type<T> t = getType(typeName);
        if (t == null) {
            t = declare(typeName, javaType);
        }
        return t;
    }

    default <T> Type<T> getOrDeclare(String typeName, String javaType) {
        Type<T> t = getType(typeName);
        if (t == null) {
            t = declare(typeName, javaType);
        }
        return t;
    }

    default <T> Type<T> getOrDeclare(String typeName) {
        return getOrDeclare(typeName, typeName);
    }

    default <T> Type<T> getOrDeclare(Class<T> cl) {
        return getOrDeclare(cl.getName(), cl);
    }

    default <T> Type<T> declare(Class<T> type) {
        return declare(type.getName(), type);
    }

    <T> Type<T> resolve(Object o);

}
