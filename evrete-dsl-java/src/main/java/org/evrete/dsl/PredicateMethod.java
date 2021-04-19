package org.evrete.dsl;

import org.evrete.api.IntToValue;
import org.evrete.api.ValuesPredicate;

abstract class PredicateMethod extends MethodWithValues implements ValuesPredicate {

    PredicateMethod(MethodWithValues method) {
        super(method);
    }

    static PredicateMethod factory(MethodWithValues method) {
        if (method.staticMethod) {
            return new Static(method);
        } else {
            return new NonStatic(method);
        }
    }

    abstract void init(IntToValue values);

    @Override
    public final boolean test(IntToValue values) {
        init(values);
        return call();
    }

    private static class NonStatic extends PredicateMethod {

        private NonStatic(MethodWithValues method) {
            super(method);
        }

        @Override
        void init(IntToValue values) {
            for (int i = 1; i < methodCurrentValues.length; i++) {
                this.methodCurrentValues[i] = values.apply(i - 1);
            }
        }
    }

    private static class Static extends PredicateMethod {

        private Static(MethodWithValues method) {
            super(method);
        }

        @Override
        void init(IntToValue values) {
            for (int i = 0; i < methodCurrentValues.length; i++) {
                this.methodCurrentValues[i] = values.apply(i);
            }
        }
    }
}
