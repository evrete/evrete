package org.evrete.api;

import java.util.Collection;

public class TypeResolverWrapper implements TypeResolver {
    private final TypeResolver delegate;

    public TypeResolverWrapper(TypeResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public Type getType(String name) {
        return delegate.getType(name);
    }

    protected TypeResolver getDelegate() {
        return delegate;
    }

    @Override
    public Type resolve(Object o) {
        return delegate.resolve(o);
    }

    @Override
    public Collection<Type> getKnownTypes() {
        return delegate.getKnownTypes();
    }

    @Override
    public Type declare(String typeName) {
        return delegate.declare(typeName);
    }

    @Override
    public TypeResolver copyOf() {
        return new TypeResolverWrapper(this);
    }
}
