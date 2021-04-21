package org.evrete.runtime.evaluation;

import org.evrete.api.Evaluator;
import org.evrete.api.FieldReference;
import org.evrete.api.IntToValue;
import org.evrete.api.ValuesPredicate;

import java.util.Arrays;

public class EvaluatorOfPredicate implements Evaluator {
    private final FieldReference[] descriptor;
    private final ValuesPredicate predicate;

    public EvaluatorOfPredicate(ValuesPredicate predicate, FieldReference... descriptor) {
        this.descriptor = descriptor;
        this.predicate = predicate;
    }

    @Override
    public FieldReference[] descriptor() {
        return descriptor;
    }

    @Override
    public boolean test(IntToValue values) {
        return predicate.test(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluatorOfPredicate that = (EvaluatorOfPredicate) o;
        return Arrays.equals(descriptor, that.descriptor) &&
                predicate.equals(that.predicate);
    }

    @Override
    public int hashCode() {
        int result = predicate.hashCode();
        result = 31 * result + Arrays.hashCode(descriptor);
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "descriptor=" + Arrays.toString(descriptor) +
                ", predicate=" + predicate +
                '}';
    }
}
