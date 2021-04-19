package org.evrete.dsl;

import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.util.KnowledgeWrapper;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DSLKnowledge extends KnowledgeWrapper {
    private final MethodWithValues[] nonStaticArray;
    private final Class<?> rulesetClass;
    private final Constructor<?> constructor;
    private final Listeners listeners = new Listeners();

    DSLKnowledge(Knowledge delegate, Class<?> rulesetClass, List<RuleMethod> ruleMethods, Collection<PredicateMethod> predicateMethods, Collection<ListenerMethod> listenerMethods) {
        super(delegate);
        this.rulesetClass = rulesetClass;
        try {
            this.constructor = rulesetClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MalformedResourceException("Unable to locate a zero-arg public constructor in " + rulesetClass.getName(), e);
        }


        // Non-static methods must be initialized on createSession()
        Collection<MethodWithValues> nonStatic = new ArrayList<>();
        for (MethodWithValues m : ruleMethods) {
            if (!m.staticMethod) {
                nonStatic.add(m);
            }
        }

        for (MethodWithValues m : predicateMethods) {
            if (!m.staticMethod) {
                nonStatic.add(m);
            }
        }

        for (ListenerMethod m : listenerMethods) {
            if (!m.staticMethod) {
                nonStatic.add(m);
            }
            for (Phase phase : m.phases) {
                listeners.add(phase, m);
            }
        }

        this.nonStaticArray = nonStatic.toArray(MethodWithValues.EMPTY);

        // There is one listener that should be called right now
        listeners.fire(Phase.BUILD, this);
    }

    @Override
    public StatefulSession createSession() {
        // Initialize non-static method handlers with class instance
        // (the first of their invocation arguments)
        if (nonStaticArray.length > 0) {
            Object instance = classInstance();
            for (MethodWithValues m : nonStaticArray) {
                m.setInstance(instance);
            }
        }

        // Fire CREATE listeners
        listeners.fire(Phase.CREATE, this);
        return new DSLSession(super.createSession(), listeners);
    }

    private Object classInstance() {
        try {
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new MalformedResourceException("Unable to create instance of " + rulesetClass.getName(), t);
        }
    }
}
