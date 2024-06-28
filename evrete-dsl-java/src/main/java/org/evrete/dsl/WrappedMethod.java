package org.evrete.dsl;

import org.evrete.api.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

class WrappedMethod {
    private final MethodHandle handle;
    final boolean isStatic;
    final Object[] args;
    private final String methodName;
    final WrappedClass declaringClass;

    WrappedMethod(WrappedClass declaringClass, MethodHandle handle, String methodName, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.handle = handle;
        this.isStatic = isStatic;
        this.methodName = methodName;
        int paramCount = handle.type().parameterCount();
        this.args = new Object[paramCount];
    }

    WrappedMethod(WrappedClass declaringClass, @NonNull Method delegate) {
        this(declaringClass, declaringClass.getHandle(delegate), delegate.getName(), Modifier.isStatic(delegate.getModifiers()));
    }

    WrappedMethod(WrappedMethod other, Object bindInstance) {
        this(other.declaringClass, other.handle.bindTo(bindInstance), other.methodName, other.isStatic);
    }

    WrappedMethod(WrappedMethod other) {
        this(other.declaringClass, other.handle, other.methodName, other.isStatic);
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
        } catch (Throwable t) {
            String[] argTypes = new String[args.length];
            for (int i = 0; i < argTypes.length; i++) {
                Object arg = args[i];
                argTypes[i] = arg == null ? null : arg.getClass().getName();
            }
            throw new RuntimeException("Method invocation exception at " + this + ", arguments: " + Arrays.toString(args) + " with types: " + Arrays.toString(argTypes), t);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "name=" + methodName +
                ", handle=" + handle +
                ", static=" + isStatic +
                ", class='" + declaringClass.delegate.getName() +
                "'}'";
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
