package org.evrete.dsl;

import org.evrete.dsl.annotation.EnvironmentListener;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class EnvListenerMethod extends ClassMethod implements SessionCloneable<EnvListenerMethod> {

    EnvListenerMethod(MethodHandles.Lookup lookup, Method method) {
        super(lookup, method);
        Parameter[] parameters = method.getParameters();
        if (parameters.length != 1) {
            throw new MalformedResourceException("Method " + method + " in the " + method.getDeclaringClass() + " is annotated as " + EnvironmentListener.class.getSimpleName()  + " but has zero or more than one parameters");
        }
    }

    private EnvListenerMethod(EnvListenerMethod method, Object instance) {
        super(method, instance);
    }

    @Override
    public EnvListenerMethod copy(Object sessionInstance) {
        return new EnvListenerMethod(this, sessionInstance);
    }

    void call(Object value, boolean staticOnly) {
        if (!staticOnly || staticMethod) {
            this.args[0] = value;
            call();
        }
    }
}
