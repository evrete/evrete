package org.evrete.runtime.evaluation;

import org.evrete.api.IntToValue;
import org.evrete.api.ValuesPredicate;

import java.util.function.Predicate;

public class ValuePredicateOfArray implements ValuesPredicate {
    private final Predicate<Object[]> predicate;
    private final Object[] sharedValues;

    public ValuePredicateOfArray(Predicate<Object[]> predicate, int arraySize) {
        this.predicate = predicate;
        this.sharedValues = new Object[arraySize];
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
        ValuePredicateOfArray that = (ValuePredicateOfArray) o;
        return predicate.equals(that.predicate);
    }

    @Override
    public int hashCode() {
        return predicate.hashCode();
    }

    @Override
    public String toString() {
        return predicate.toString();
    }
}
