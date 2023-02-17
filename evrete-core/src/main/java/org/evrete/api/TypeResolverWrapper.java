package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;

public class TypeResolverWrapper implements TypeResolver {
    private final TypeResolver delegate;

    public TypeResolverWrapper(TypeResolver delegate) {
        this.delegate = delegate;
    }

    @NonNull
    @Override
    public <T> Type<T> getType(int typeId) {
        return delegate.getType(typeId);
    }

    @Override
    public void wrapType(TypeWrapper<?> typeWrapper) {
        delegate.wrapType(typeWrapper);
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
    @NonNull
    public <T> Type<T> declare(@NonNull String typeName, @NonNull String javaType) {
        return delegate.declare(typeName, javaType);
    }

    @Override
    public TypeResolver copyOf() {
        return new TypeResolverWrapper(delegate.copyOf());
    }

    @NonNull
    @Override
    public <T> Type<T> declare(@NonNull String typeName, @NonNull Class<T> javaType) {
        return delegate.declare(typeName, javaType);
    }
}
