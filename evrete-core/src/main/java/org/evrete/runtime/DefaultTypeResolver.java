package org.evrete.runtime;

import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.api.TypeStorage;
import org.evrete.util.TypeWrapper;

import java.util.Collection;
import java.util.function.Supplier;

class DefaultTypeResolver implements TypeResolver {
    private final TypeStorage typeStorage;
    private final Supplier<ClassLoader> classLoaderSupplier;

    DefaultTypeResolver(TypeStorage typeStorage, Supplier<ClassLoader> classLoaderSupplier) {
        this.typeStorage = typeStorage;
        this.classLoaderSupplier = classLoaderSupplier;
    }

    public DefaultTypeResolver clone(Supplier<ClassLoader> classLoaderSupplier) {
        return new DefaultTypeResolver(typeStorage.copyOf(), classLoaderSupplier);
    }

    @Override
    public <T> Type<T> getType(String name) {
        return typeStorage.getType(name);
    }

    @Override
    public <T> Type<T> getType(int typeId) {
        return typeStorage.getType(typeId);
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return typeStorage.getKnownTypes();
    }

    @Override
    public void wrapType(TypeWrapper<?> typeWrapper) {
        typeStorage.wrapType(typeWrapper);
    }

    @Override
    public <T> Type<T> declare(String typeName, Class<T> javaType) {
        return typeStorage.declare(typeName, javaType);
    }

    @Override
    public <T> Type<T> declare(String typeName, String javaType) {
        ClassLoader classLoader = classLoaderSupplier.get();
        return typeStorage.declare(classLoader, typeName, javaType);
    }

    @Override
    public <T> Type<T> resolve(Object o) {
        return typeStorage.resolve(o);
    }
}
