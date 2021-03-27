package org.evrete.runtime.builder;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;

import java.util.Arrays;
import java.util.function.Function;

class PredicateExpression4 extends AbstractExpression {
    private final ValuesPredicate predicate;
    private final FieldReference[] references;

    PredicateExpression4(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        super(complexity);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.predicate = predicate;
        this.references = references;
    }

    PredicateExpression4(ValuesPredicate predicate, FieldReference[] references) {
        this(predicate, DEFAULT_COMPLEXITY, references);
    }

    @Override
    Evaluator build(AbstractRuntime<?, ?> runtime, Function<String, NamedType> typeMapper) {
        return new PredicateEvaluator(references, predicate, getComplexity());
    }

    private static class PredicateEvaluator implements Evaluator {
        private final FieldReference[] descriptor;
        private final ValuesPredicate predicate;
        private final double complexity;

        PredicateEvaluator(FieldReference[] descriptor, ValuesPredicate predicate, double complexity) {
            this.descriptor = descriptor;
            this.predicate = predicate;
            this.complexity = complexity;
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
    }
}
