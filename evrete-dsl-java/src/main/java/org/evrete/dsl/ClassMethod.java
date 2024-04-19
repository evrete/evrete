package org.evrete.dsl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

class ClassMethod {
    final boolean staticMethod;
    final Object[] args;
    private final MethodHandle handle;

    ClassMethod(MethodHandles.Lookup lookup, Method method) {
        this.staticMethod = Modifier.isStatic(method.getModifiers());
        try {
            this.handle = lookup.in(method.getDeclaringClass()).unreflect(method);
            this.args = new Object[handle.type().parameterCount()];
        } catch (IllegalAccessException e) {
            throw new MalformedResourceException("Rule method access exception", e);
        }
    }

    private ClassMethod(boolean staticMethod, MethodHandle handle) {
        this.staticMethod = staticMethod;
        this.handle = handle;
        this.args = new Object[handle.type().parameterCount()];
    }

    ClassMethod(ClassMethod method, Object instance) {
        this.staticMethod = method.staticMethod;
        this.handle = staticMethod ? method.handle : method.handle.bindTo(instance);
        this.args = new Object[this.handle.type().parameterCount()];
    }

    ClassMethod(ClassMethod other) {
        this.staticMethod = other.staticMethod;
        this.handle = other.handle;
        this.args = other.args.clone();
    }

    static ClassMethod lookup(MethodHandles.Lookup lookup, Class<?> declaringClass, String name, MethodType methodType) {
        MethodHandle handle;
        boolean staticMethod;
        MethodHandles.Lookup declaringClassLookup = lookup.in(declaringClass);
        try {
            handle = declaringClassLookup.findStatic(declaringClass, name, methodType);
            staticMethod = true;
        } catch (NoSuchMethodException | IllegalAccessException e1) {
            try {
                handle = declaringClassLookup.findVirtual(declaringClass, name, methodType);
                staticMethod = false;
            } catch (NoSuchMethodException | IllegalAccessException e2) {
                throw new MalformedResourceException("Unable to find/access method '" + name + "' in " + declaringClass + " of type " + methodType);
            }
        }
        return new ClassMethod(staticMethod, handle);
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

    final <V, R> Function<V, R> asFunction() {
        //noinspection Convert2Diamond
        return new Function<V,R>() {
            @Override
            public R apply(V v) {
                args[0] = v;
                return call();
            }

            @Override
            public String toString() {
                return handle.toString();
            }
        };
    }
}
