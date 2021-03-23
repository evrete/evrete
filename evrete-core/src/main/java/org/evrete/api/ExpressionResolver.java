package org.evrete.api;

import org.evrete.util.compiler.CompilationException;

import java.util.Set;
import java.util.function.Function;

public interface ExpressionResolver {

    //TODO !!!! rename, javadoc and return null if not resolved
    FieldReference resolve(String s, Function<String, NamedType> resolver);

    Evaluator buildExpression(String stringExpression, Function<String, NamedType> resolver, Set<String> imports) throws CompilationException;
}
