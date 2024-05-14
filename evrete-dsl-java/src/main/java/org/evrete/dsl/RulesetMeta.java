package org.evrete.dsl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class RulesetMeta {
    final PhaseListeners phaseListeners = new PhaseListeners();
    final EnvListeners envListeners = new EnvListeners();
    final FieldDeclarations fieldDeclarations = new FieldDeclarations();
    final MethodHandles.Lookup lookup;
    final Class<?> javaClass;
    List<RuleMethod> ruleMethods = new ArrayList<>();

    RulesetMeta(MethodHandles.Lookup lookup, Class<?> javaClass) {
        this.lookup = lookup;
        this.javaClass = javaClass;
    }

    void addPhaseListener(Method m) {
        PhaseListenerMethod lm = new PhaseListenerMethod(lookup, m);
        phaseListeners.add(lm);
    }

    void addRuleMethod(Method m) {
        ruleMethods.add(new RuleMethod(lookup, m));
    }

    void addFieldDeclaration(Method method, String type) {
        fieldDeclarations.addFieldDeclaration(new FieldDeclarationMethod<>(lookup, method, type));
    }

    void addEnvListener(Method method, String property) {
        envListeners.add(property, new EnvListenerMethod(lookup, method));
    }

}
