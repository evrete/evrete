package org.evrete.dsl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

class MethodWithValues {
    static final MethodWithValues[] EMPTY = new MethodWithValues[0];
    final boolean staticMethod;
    private final MethodHandle handle;
    final Object[] methodCurrentValues;

    MethodWithValues(MethodHandles.Lookup lookup, Method method) {
        this.staticMethod = Modifier.isStatic(method.getModifiers());
        try {
            this.handle = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new MalformedResourceException("Rule method access exception", e);
        }
        Parameter[] parameters = method.getParameters();

        if (staticMethod) {
            this.methodCurrentValues = new Object[parameters.length];
        } else {
            this.methodCurrentValues = new Object[parameters.length + 1];
        }
    }

    private MethodWithValues(boolean staticMethod, MethodHandle handle) {
        this.staticMethod = staticMethod;
        this.handle = handle;
        this.methodCurrentValues = new Object[handle.type().parameterArray().length];
    }

    MethodWithValues(MethodWithValues other) {
        this.staticMethod = other.staticMethod;
        this.handle = other.handle;
        this.methodCurrentValues = new Object[other.methodCurrentValues.length];
    }

    static MethodWithValues lookup(MethodHandles.Lookup lookup, Class<?> javaClass, String name, MethodType methodType) {
        MethodHandle handle;
        boolean staticMethod;
        try {
            handle = lookup.findStatic(javaClass, name, methodType);
            staticMethod = true;
        } catch (NoSuchMethodException | IllegalAccessException e1) {
            try {
                handle = lookup.findVirtual(javaClass, name, methodType);
                staticMethod = false;
            } catch (NoSuchMethodException | IllegalAccessException e2) {
                throw new MalformedResourceException("Unable to find/access method '" + name + "' in " + javaClass + " of type " + methodType);
            }
        }
        return new MethodWithValues(staticMethod, handle);
    }

    // For non-static methods
    void setInstance(Object instance) {
        if (!staticMethod) {
            this.methodCurrentValues[0] = instance;
        }
    }

    @SuppressWarnings("unchecked")
    final <T> T call() {
        try {
            return (T) handle.invokeWithArguments(methodCurrentValues);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
