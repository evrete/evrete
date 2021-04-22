package org.evrete.dsl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class RulesetMeta {
    final Listeners listeners = new Listeners();
    final FieldDeclarations fieldDeclarations = new FieldDeclarations();
    final MethodHandles.Lookup lookup;
    final Class<?> javaClass;
    List<RuleMethod> ruleMethods = new ArrayList<>();

    RulesetMeta(Class<?> javaClass) {
        this.lookup = MethodHandles.lookup().in(javaClass);
        ;
        this.javaClass = javaClass;
    }

    void addListener(Method m) {
        ListenerMethod lm = new ListenerMethod(lookup, m);
        listeners.add(lm);
    }

    void addRuleMethod(Method m) {
        ruleMethods.add(new RuleMethod(lookup, m));
    }

    void addFieldDeclaration(Method method) {

        //FieldDeclarationMethod<?, ?> fieldMethod = new FieldDeclarationMethod<>(lookup, m);
        //fieldMethod.declareInitialField(knowledge.getTypeResolver());

        fieldDeclarations.addFieldDeclaration(new FieldDeclarationMethod<>(lookup, method));
    }
}
