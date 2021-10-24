package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.Environment;
import org.evrete.dsl.annotation.PhaseListener;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

class PhaseListenerMethod extends ClassMethod implements SessionCloneable<PhaseListenerMethod> {
    final Set<Phase> phases = EnumSet.noneOf(Phase.class);
    private int configIndex;
    private int environmentIndex;

    PhaseListenerMethod(MethodHandles.Lookup lookup, Method method) {
        super(lookup, method);
        PhaseListener listener = Objects.requireNonNull(method.getAnnotation(PhaseListener.class));
        Collections.addAll(phases, listener.value());
        if (!method.getReturnType().equals(void.class)) {
            throw new MalformedResourceException("Listener methods must be void. " + method);
        }

        if (phases.contains(Phase.BUILD) && !staticMethod) {
            throw new MalformedResourceException(Phase.BUILD + " is bound to a non-static method " + method + " of " + method.getDeclaringClass());
        }

        this.configIndex = -1;
        this.environmentIndex = -1;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (Configuration.class.isAssignableFrom(type)) {
                if (configIndex < 0) {
                    configIndex = i;
                } else {
                    throw new MalformedResourceException("Duplicate configuration argument in " + method);
                }
            } else if (Environment.class.isAssignableFrom(type)) {
                if (environmentIndex < 0) {
                    environmentIndex = i;
                } else {
                    throw new MalformedResourceException("Duplicate environment argument in " + method);
                }
            } else {
                throw new MalformedResourceException("Illegal argument type in " + method + " : " + type);
            }
        }
    }

    private PhaseListenerMethod(PhaseListenerMethod method, Object instance) {
        super(method, instance);
        this.configIndex = method.configIndex;
        this.environmentIndex = method.environmentIndex;
    }

    void call(Configuration configuration, Environment environment) {
        if (configIndex >= 0) {
            this.args[configIndex] = configuration;
        }
        if (environmentIndex >= 0) {
            this.args[environmentIndex] = environment;
        }
        call();
    }

    @Override
    public PhaseListenerMethod copy(Object sessionInstance) {
        return new PhaseListenerMethod(this, sessionInstance);
    }
}
