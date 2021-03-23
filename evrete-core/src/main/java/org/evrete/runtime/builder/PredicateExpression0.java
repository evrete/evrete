package org.evrete.runtime.builder;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.Imports;

import java.util.Objects;
import java.util.function.Function;

class PredicateExpression0 extends AbstractExpression {
    private final String source;
    private final Imports imports;

    PredicateExpression0(String source, double complexity, Imports imports) {
        super(complexity);
        this.imports = imports;
        Objects.requireNonNull(source);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.source = source;
    }

    PredicateExpression0(String source, Imports imports) {
        this(source, DEFAULT_COMPLEXITY, imports);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PredicateExpression0 that = (PredicateExpression0) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }


    @Override
    Evaluator build(AbstractRuntime<?> runtime, Function<String, NamedType> typeMapper) {
        Evaluator e = runtime.compile(source, typeMapper, imports.get(RuleScope.BOTH, RuleScope.LHS));
        double complexity = getComplexity();
        if (complexity == ComplexityObject.DEFAULT_COMPLEXITY) {
            return e;
        } else {
            return new Evaluator() {
                @Override
                public FieldReference[] descriptor() {
                    return e.descriptor();
                }

                @Override
                public boolean test(IntToValue intToValue) {
                    return e.test(intToValue);
                }

                @Override
                public int compare(Evaluator other) {
                    return e.compare(other);
                }

                @Override
                public String toString() {
                    return e.toString();
                }

                @Override
                public double getComplexity() {
                    return complexity;
                }
            };
        }
    }
}
