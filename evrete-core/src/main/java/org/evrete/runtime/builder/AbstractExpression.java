package org.evrete.runtime.builder;

import org.evrete.api.ComplexityObject;
import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.api.spi.ExpressionResolver;
import org.evrete.runtime.AbstractRuntime;

import java.util.function.Function;

public abstract class AbstractExpression implements ComplexityObject {
    private final double complexity;

    protected AbstractExpression(double complexity) {
        this.complexity = complexity;
    }

    @Override
    public final double getComplexity() {
        return complexity;
    }

    abstract Evaluator build(AbstractRuntime<?> runtime, Function<String, NamedType> typeMapper);


    static FieldReference[] resolveReferences(AbstractRuntime<?> runtime, Function<String, NamedType> typeMapper, String[] references) {
        FieldReference[] descriptor = new FieldReference[references.length];
        ExpressionResolver expressionResolver = runtime.getConfiguration().getExpressionsService();
        for (int i = 0; i < descriptor.length; i++) {
            descriptor[i] = expressionResolver.resolve(references[i], typeMapper);
        }
        return descriptor;
    }
}
