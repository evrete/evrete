package org.evrete.runtime.builder;

import org.evrete.api.Evaluator;
import org.evrete.api.FieldReference;
import org.evrete.api.IntToValue;
import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

class PredicateExpression2 extends AbstractExpression {
    private final Predicate<Object[]> predicate;
    private final String[] references;

    PredicateExpression2(Predicate<Object[]> predicate, double complexity, String[] references) {
        super(complexity);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.predicate = predicate;
        this.references = references;
    }

    PredicateExpression2(Predicate<Object[]> predicate, String[] references) {
        this(predicate, DEFAULT_COMPLEXITY, references);
    }

    @Override
    Evaluator build(AbstractRuntime<?, ?> runtime, Function<String, NamedType> typeMapper) {
        FieldReference[] descriptor = resolveReferences(runtime, typeMapper, references);
        return new PredicateEvaluator(descriptor, predicate, getComplexity());
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
            return 31 * predicate.hashCode() + Arrays.hashCode(descriptor);
        }

        @Override
        public double getComplexity() {
            return complexity;
        }

        @Override
        public String toString() {
            return predicate.toString();
        }
    }
}
