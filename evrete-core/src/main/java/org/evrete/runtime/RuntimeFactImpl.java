package org.evrete.runtime;

import org.evrete.api.RuntimeFact;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.Arrays;

public final class RuntimeFactImpl implements RuntimeFact {
    private static final boolean[] EMPTY_ALPHA_TESTS = new boolean[0];
    private final Object delegate;
    private Object[] values;
    private boolean[] alphaTests;
    private boolean deleted = false;

    private RuntimeFactImpl(Object o, Object[] values) {
        this(o, values, EMPTY_ALPHA_TESTS);
    }

    private RuntimeFactImpl(Object o, Object[] values, boolean[] alphaTests) {
        this.values = values;
        this.delegate = o;
        this.alphaTests = alphaTests;
    }

    public static RuntimeFactImpl factory(Object o, Object[] values, boolean[] alphaTests) {
        return new RuntimeFactImpl(o, values, alphaTests);
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public static RuntimeFactImpl factory(Object o, Object[] values) {
        return new RuntimeFactImpl(o, values);
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

    public final void appendValue(ActiveField field, Object value) {
        assert values.length == field.getValueIndex();
        this.values = Arrays.copyOf(this.values, values.length + 1);
        this.values[field.getValueIndex()] = value;
    }

    public final void appendAlphaTest(AlphaEvaluator[] newEvaluators) {
        int currentSize = this.alphaTests.length;
        this.alphaTests = Arrays.copyOf(this.alphaTests, currentSize + newEvaluators.length);
        for (int i = 0; i < newEvaluators.length; i++) {
            int newIndex = currentSize + i;
            AlphaEvaluator newEvaluator = newEvaluators[i];
            assert newIndex == newEvaluator.getUniqueId();
            this.alphaTests[newIndex] = newEvaluator.test(this.values);
        }
    }

    public String toString() {
        return "{delegate=" + delegate +
                ", deleted=" + deleted +
                //", tests=" + Arrays.toString(alphaTests) +
                '}';
    }
}
