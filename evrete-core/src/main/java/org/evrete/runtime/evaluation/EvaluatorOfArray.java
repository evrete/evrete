package org.evrete.runtime.evaluation;

import org.evrete.api.Evaluator;
import org.evrete.api.FieldReference;
import org.evrete.api.IntToValue;

import java.util.Arrays;
import java.util.function.Predicate;

public class EvaluatorOfArray implements Evaluator {
    private final FieldReference[] descriptor;
    private final Predicate<Object[]> predicate;
    private final Object[] sharedValues;

    public EvaluatorOfArray(Predicate<Object[]> predicate, FieldReference[] descriptor) {
        this.descriptor = descriptor;
        this.predicate = predicate;
        this.sharedValues = new Object[descriptor.length];
    }

    @Override
    public FieldReference[] descriptor() {
        return descriptor;
    }

    @Override
    public boolean test(IntToValue values) {
        synchronized (sharedValues) {
            for (int i = 0; i < sharedValues.length; i++) {
                sharedValues[i] = values.apply(i);
            }
            return predicate.test(sharedValues);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluatorOfArray that = (EvaluatorOfArray) o;
        return Arrays.equals(descriptor, that.descriptor) &&
                predicate.equals(that.predicate);
    }

    @Override
    public int hashCode() {
        return 31 * predicate.hashCode() + Arrays.hashCode(descriptor);
    }

    @Override
    public String toString() {
        return predicate.toString();
    }
}
