package org.evrete.runtime.builder;

import org.evrete.api.ComplexityObject;
import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;

import java.util.function.Function;


public abstract class AbstractExpression implements ComplexityObject {
    private final double complexity;

    AbstractExpression(double complexity) {
        this.complexity = complexity;
    }

    @Override
    public final double getComplexity() {
        return complexity;
    }

    abstract Evaluator build(AbstractRuntime<?, ?> runtime, Function<String, NamedType> typeMapper);
}
