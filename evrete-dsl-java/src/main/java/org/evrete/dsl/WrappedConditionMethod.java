package org.evrete.dsl;

import org.evrete.api.IntToValue;
import org.evrete.api.ValuesPredicate;

class WrappedConditionMethod extends WrappedCloneableMethod<WrappedConditionMethod> implements ValuesPredicate {

    WrappedConditionMethod(WrappedMethod other) {
        super(other);
    }

    WrappedConditionMethod(WrappedMethod other, Object bindInstance) {
        super(other, bindInstance);
    }

    @Override
    public boolean test(IntToValue values) {
        for (int i = 0; i < args.length; i++) {
            this.args[i] = values.apply(i);
        }
        return call();
    }

    @Override
    public WrappedConditionMethod bindTo(Object instance) {
        return new WrappedConditionMethod(this, instance);
    }
}
