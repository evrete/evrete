package org.evrete.util;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.function.Function;

/**
 * A wrapper class that implements the {@link Type} interface and delegates the calls to another Type implementation.
 *
 * @param <T> The Java type associated with this type.
 */
public class TypeWrapper<T> implements Type<T> {
    private final Type<T> delegate;

    public TypeWrapper(Type<T> delegate) {
        this.delegate = delegate;
    }

    public final Type<T> getDelegate() {
        return delegate;
    }

    @Override
    @Deprecated
    public final String getJavaType() {
        return delegate.getJavaType();
    }

    @Override
    @Deprecated
    public Class<T> resolveJavaType() {
        return delegate.getJavaClass();
    }

    @Override
    public final String getName() {
        return delegate.getName();
    }

    @Override
    public Collection<TypeField> getDeclaredFields() {
        return delegate.getDeclaredFields();
    }

    @Override
    public @NonNull TypeField getField(@NonNull String name) {
        return delegate.getField(name);
    }

    @Override
    public Class<T> getJavaClass() {
        return delegate.getJavaClass();
    }

    @Override
    public <V> TypeField declareField(String name, Class<V> type, Function<T, V> function) {
        return delegate.declareField(name, type, function);
    }

    @Override
    public Type<T> copyOf() {
        return new TypeWrapper<>(delegate.copyOf());
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof Type<?>) {
            Type<?> that = (Type<?>) o;
            return getName().equals(that.getName());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }
}
