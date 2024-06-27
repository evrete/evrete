package org.evrete.dsl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

class WrappedClass {
    protected final Class<?> delegate;
    protected final Method[] publicMethods;
    private final MethodHandles.Lookup classLookup;

    WrappedClass(Class<?> delegate, MethodHandles.Lookup publicLookup) {
        this.delegate = delegate;
        this.classLookup = publicLookup.in(delegate);
        this.publicMethods = delegate.getMethods();
    }

    protected WrappedClass(WrappedClass other) {
        this.delegate = other.delegate;
        this.publicMethods = other.publicMethods;
        this.classLookup = other.classLookup;
    }

    MethodHandle getHandle(Method m) {
        try {
            return classLookup.unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access method " + m, e);
        }
    }

    WrappedMethod lookup(String name, MethodType methodType) {
        MethodHandle handle;
        boolean staticMethod;
        try {
            handle = classLookup.findStatic(delegate, name, methodType);
            staticMethod = true;
        } catch (NoSuchMethodException | IllegalAccessException e1) {
            try {
                handle = classLookup.findVirtual(delegate, name, methodType);
                staticMethod = false;
            } catch (NoSuchMethodException | IllegalAccessException e2) {
                throw new MalformedResourceException("Unable to find/access method '" + name + "' in " + delegate + " of type " + methodType);
            }
        }

        return new WrappedMethod(this, handle, staticMethod);
    }
}
