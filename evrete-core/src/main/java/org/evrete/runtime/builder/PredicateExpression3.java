package org.evrete.runtime.builder;

import org.evrete.api.Evaluator;
import org.evrete.api.IntToValue;
import org.evrete.api.LogicallyComparable;
import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class PredicateExpression3 extends AbstractExpression {
    private final Predicate<Object[]> predicate;
    private final FieldReference[] references;

    public PredicateExpression3(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        super(complexity);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.predicate = predicate;
        this.references = references;
    }

    public PredicateExpression3(Predicate<Object[]> predicate, FieldReference[] references) {
        this(predicate, DEFAULT_COMPLEXITY, references);
    }

    @Override
    Evaluator build(AbstractRuntime<?, ?> runtime, Function<String, NamedType> typeMapper) {
        return new PredicateEvaluator(references, predicate, getComplexity());
    }

    private static class PredicateEvaluator implements Evaluator {
        private final FieldReference[] descriptor;
        private final Predicate<Object[]> predicate;
        private final double complexity;
        private final Object[] sharedValues;

        PredicateEvaluator(FieldReference[] descriptor, Predicate<Object[]> predicate, double complexity) {
            this.descriptor = descriptor;
            this.predicate = predicate;
            this.complexity = complexity;
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
            PredicateEvaluator that = (PredicateEvaluator) o;
            return Arrays.equals(descriptor, that.descriptor) &&
                    predicate.equals(that.predicate);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(predicate);
            result = 31 * result + Arrays.hashCode(descriptor);
            return result;
        }

        @Override
        public double getComplexity() {
            return complexity;
        }

        @Override
        public int compare(LogicallyComparable other) {
            if (other.equals(this)) return LogicallyComparable.RELATION_EQUALS;
            return LogicallyComparable.RELATION_NONE;
        }
    }
}
