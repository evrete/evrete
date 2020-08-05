package org.evrete.runtime.builder;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class PredicateExpression1 extends AbstractExpression {
    private final ValuesPredicate predicate;
    private final String[] references;

    public PredicateExpression1(ValuesPredicate predicate, double complexity, String[] references) {
        super(complexity);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.predicate = predicate;
        this.references = references;
    }

    public PredicateExpression1(ValuesPredicate predicate, String[] references) {
        this(predicate, DEFAULT_COMPLEXITY, references);
    }

    @Override
    Evaluator build(AbstractRuntime<?, ?> runtime, Function<String, NamedType> typeMapper) {
        FieldReference[] descriptor = resolveReferences(runtime, typeMapper, references);
        return new PredicateEvaluator(descriptor, predicate, getComplexity());
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
