package org.evrete.api.spi;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.builder.FieldReference;

import java.util.function.Function;

public interface ExpressionResolver {

    FieldReference resolve(String s, Function<String, NamedType> resolver);

    Evaluator buildExpression(String stringExpression, Function<String, NamedType> resolver);
}
