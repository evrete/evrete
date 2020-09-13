package org.evrete.api;

import org.evrete.runtime.builder.FieldReference;

import java.util.function.Function;

public interface ExpressionResolver {

    FieldReference resolve(String s, Function<String, NamedType> resolver);

    Evaluator buildExpression(String stringExpression, Function<String, NamedType> resolver);
}
