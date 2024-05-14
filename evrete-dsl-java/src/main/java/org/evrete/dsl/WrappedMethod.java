package org.evrete.dsl;

import org.evrete.api.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

abstract class WrappedMethod {
    private final MethodHandle handle;
    final Parameter[] parameters;

    WrappedMethod(MethodHandles.Lookup lookup, @NonNull Method delegate) {
        try {
            this.handle = lookup.unreflect(delegate);
            this.parameters = delegate.getParameters();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to process method " + delegate, e);
        }
    }

    public WrappedMethod(WrappedMethod other) {
        this.handle = other.handle;
        this.parameters = other.parameters;
    }
}
