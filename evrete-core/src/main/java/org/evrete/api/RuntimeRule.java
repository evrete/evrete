package org.evrete.api;

import org.evrete.util.compiler.CompilationException;

public interface RuntimeRule extends Rule {

    RuleSession<?> getRuntime();


    /**
     * <p>
     * Compiles a string expression and returns it as an {@link Evaluator} instance.
     * </p>
     *
     * @param expression expression to compile
     * @return evaluator instance
     */
    default Evaluator buildExpression(String expression) {
        try {
            ExpressionResolver resolver = getRuntime().getExpressionResolver();
            return resolver.buildExpression(expression, this, getRuntime().getImports().get());
        } catch (CompilationException e) {
            throw new IllegalArgumentException("Unable to compile expression '" + expression + "'", e);
        }
    }

    default FieldReference[] resolve(String[] args) {
        return getRuntime().resolveFieldReferences(args, this);
    }
}
