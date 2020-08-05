package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.RuntimeFact;

import java.util.Arrays;

public abstract class RuntimeObject implements RuntimeFact {
    private final Object[] values;
    private final Object delegate;

    protected RuntimeObject(Object o, Object[] values) {
        this.values = values;
        this.delegate = o;
    }

    @Override
    public Object apply(ActiveField field) {
        return values[field.getValueIndex()];
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    @Override
    public Object[] getValues() {
        return values;
    }

    public static RuntimeObject factory(Object o, Object[] values, boolean[] alphaTests) {
        return new WithAlpha(o, values, alphaTests);
    }

    public static RuntimeObject factory(Object o, Object[] values) {
        return new NoAlpha(o, values);
    }

    static class NoAlpha extends RuntimeObject {
        private static final boolean[] NO_TESTS = new boolean[0];

        public NoAlpha(Object o, Object[] values) {
            super(o, values);
        }

        @Override
        public boolean[] getAlphaTests() {
            return NO_TESTS;
        }

        @Override
        public String toString() {
            return "{delegate=" + getDelegate() +
                    ", values=" + Arrays.toString(getValues()) +
                    '}';
        }

    }

    static class WithAlpha extends RuntimeObject {
        private final boolean[] alphaTests;

        public WithAlpha(Object o, Object[] values, boolean[] alphaTests) {
            super(o, values);
            this.alphaTests = alphaTests;
        }

        @Override
        public boolean[] getAlphaTests() {
            return alphaTests;
        }

        @Override
        public String toString() {
            return "{delegate=" + getDelegate() +
                    ", values=" + Arrays.toString(getValues()) +
                    ", tests=" + Arrays.toString(alphaTests) +
                    '}';
        }
    }

}
