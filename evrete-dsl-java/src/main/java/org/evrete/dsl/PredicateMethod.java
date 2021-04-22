package org.evrete.dsl;

import org.evrete.api.Evaluator;
import org.evrete.api.EvaluatorHandle;
import org.evrete.api.FieldReference;
import org.evrete.api.IntToValue;

class PredicateMethod extends ClassMethod implements Evaluator, SessionCloneable<PredicateMethod> {
    final EvaluatorHandle handle;
    private final FieldReference[] references;

    PredicateMethod(ClassMethod method, FieldReference[] references, EvaluatorHandle handle) {
        super(method);
        this.handle = handle;
        this.references = references;
    }

    private PredicateMethod(PredicateMethod m, Object instance) {
        super(m, instance);
        this.handle = m.handle;
        this.references = m.references;
    }

    @Override
    public FieldReference[] descriptor() {
        return references;
    }

    @Override
    public PredicateMethod copy(Object sessionInstance) {
        return new PredicateMethod(this, sessionInstance);
    }

    @Override
    public final boolean test(IntToValue values) {
        for (int i = 0; i < args.length; i++) {
            this.args[i] = values.apply(i);
        }
        return call();
    }
}
