package org.evrete.api;

import java.util.function.Function;

public interface ExpressionResolver {

    //TODO !!!! rename, javadoc and return null if not resolved
    FieldReference resolve(String s, Function<String, NamedType> resolver);

    Evaluator buildExpression(String stringExpression, Function<String, NamedType> resolver);
}
