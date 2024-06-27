package org.evrete.dsl;

import org.evrete.api.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.function.Function;

class WrappedMethod {
    private final MethodHandle handle;
    final boolean isStatic;
    final Object[] args;
    final WrappedClass declaringClass;

    WrappedMethod(WrappedClass declaringClass, MethodHandle handle, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.handle = handle;
        this.isStatic = isStatic;
        int paramCount = handle.type().parameterCount();
        this.args = new Object[paramCount];
    }

    WrappedMethod(WrappedClass declaringClass, @NonNull Method delegate) {
        this(declaringClass, declaringClass.getHandle(delegate), Modifier.isStatic(delegate.getModifiers()));
    }

    WrappedMethod(WrappedMethod other, Object bindInstance) {
        this(other.declaringClass, other.handle.bindTo(bindInstance), other.isStatic);
    }

    WrappedMethod(WrappedMethod other) {
        this(other.declaringClass, other.handle, other.isStatic);
    }

    final <V, R> Function<V, R> asFunction() {
        return new Func<>();
    }

    final <R> Consumer<R> asVoidFunction() {
        return new Cons<>();
    }

    @SuppressWarnings("unchecked")
    final <T> T call() {
        try {
            return (T) handle.invokeWithArguments(args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private class Func<V, R> implements Function<V, R> {
        @Override
        public R apply(V v) {
            args[0] = v;
            return call();
        }

        @Override
        public String toString() {
            return "Func{" + handle +
                    "}";
        }
    }

    private class Cons<R> implements Consumer<R> {
        @Override
        public void accept(R r) {
            args[0] = r;
            call();
        }

        @Override
        public String toString() {
            return "Cons{" + handle +
                    "}";
        }
    }
}
