package org.evrete.api;

import java.util.Collection;

public class TypeResolverWrapper implements TypeResolver {
    private final TypeResolver delegate;

    public TypeResolverWrapper(TypeResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Type<T> getType(String name) {
        return delegate.getType(name);
    }

    @Override
    public <T> Type<T> resolve(Object o) {
        return delegate.resolve(o);
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return delegate.getKnownTypes();
    }

    @Override
    public <T> Type<T> declare(String typeName, String javaType) {
        return delegate.declare(typeName, javaType);
    }

    @Override
    public TypeResolver copyOf() {
        return new TypeResolverWrapper(this);
    }

    @Override
    public <T> Type<T> declare(String typeName, Class<T> javaType) {
        return delegate.declare(typeName, javaType);
    }
}
