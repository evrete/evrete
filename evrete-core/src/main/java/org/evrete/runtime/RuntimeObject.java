package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.RuntimeFact;

import java.util.Arrays;

public final class RuntimeObject implements RuntimeFact {
    private static final boolean[] EMPTY_ALPHA_TESTS = new boolean[0];
    private Object[] values;
    private final Object delegate;
    private boolean[] alphaTests;


    private RuntimeObject(Object o, Object[] values) {
        this(o, values, EMPTY_ALPHA_TESTS);
    }

    private RuntimeObject(Object o, Object[] values, boolean[] alphaTests) {
        this.values = values;
        this.delegate = o;
        this.alphaTests = alphaTests;
    }

    @Override
    public Object apply(ActiveField field) {
        return values[field.getValueIndex()];
    }


    @Override
    public boolean[] getAlphaTests() {
        return alphaTests;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDelegate() {
        return (T) delegate;
    }

    @Override
    public Object[] getValues() {
        return values;
    }

    public static RuntimeObject factory(Object o, Object[] values, boolean[] alphaTests) {
        return new RuntimeObject(o, values, alphaTests);
    }

    public static RuntimeObject factory(Object o, Object[] values) {
        return new RuntimeObject(o, values);
    }

    public final void appendValue(ActiveField field, Object value) {
        assert values.length == field.getValueIndex();
        this.values = Arrays.copyOf(this.values, values.length + 1);
        this.values[field.getValueIndex()] = value;
    }

    public final RuntimeObject appendAlphaTest(AlphaEvaluator newEvaluator) {
        int size = this.alphaTests.length;
        this.alphaTests = Arrays.copyOf(this.alphaTests, size + 1);
        Object fieldValue = values[newEvaluator.getValueIndex()];
        this.alphaTests[size] = newEvaluator.test(fieldValue);
        return this;
    }

    public String toString() {
        return "{delegate=" + getDelegate() +
                ", values=" + Arrays.toString(values) +
                ", tests=" + Arrays.toString(alphaTests) +
                '}';
    }
}
