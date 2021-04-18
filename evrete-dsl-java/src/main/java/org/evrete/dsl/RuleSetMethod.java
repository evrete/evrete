package org.evrete.dsl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

class RuleSetMethod {
    final boolean staticMethod;
    final Method method;
    final MethodHandle handle;
    final Object[] methodCurrentValues;

    RuleSetMethod(MethodHandles.Lookup lookup, Method method) {
        this.staticMethod = Modifier.isStatic(method.getModifiers());
        try {
            this.handle = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new MalformedResourceException("Rule method access exception", e);
        }
        this.method = method;
        Parameter[] parameters = method.getParameters();

        if (staticMethod) {
            this.methodCurrentValues = new Object[parameters.length];
        } else {
            this.methodCurrentValues = new Object[parameters.length + 1];
        }
    }

    RuleSetMethod(RuleSetMethod other) {
        this.staticMethod = other.staticMethod;
        this.method = other.method;
        this.handle = other.handle;
        this.methodCurrentValues = other.methodCurrentValues;
    }


    <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }


    Parameter[] getParameters() {
        return method.getParameters();
    }

    // For non-static methods
    void setInstance(Object instance) {
        if (!staticMethod) {
            this.methodCurrentValues[0] = instance;
        }
    }

    String getMethodName() {
        return method.getName();
    }
}
