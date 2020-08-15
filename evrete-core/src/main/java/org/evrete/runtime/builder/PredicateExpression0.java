package org.evrete.runtime.builder;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;

import java.util.Objects;
import java.util.function.Function;

class PredicateExpression0 extends AbstractExpression {
    private final String source;

    PredicateExpression0(String source, double complexity) {
        super(complexity);
        Objects.requireNonNull(source);
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");
        this.source = source;
    }

    public PredicateExpression0(String source) {
        this(source, DEFAULT_COMPLEXITY);
    }

    public String getSource() {
        return source;
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
        return Objects.hash(source);
    }


    @Override
    Evaluator build(AbstractRuntime<?> runtime, Function<String, NamedType> typeMapper) {
        return runtime.compile(source, typeMapper).withComplexity(getComplexity());
    }
}