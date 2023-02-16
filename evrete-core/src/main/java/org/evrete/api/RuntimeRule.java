package org.evrete.api;

import org.evrete.util.compiler.CompilationException;

import java.util.Properties;

public interface RuntimeRule extends Rule {

    RuleSession<?> getRuntime();

    /**
     * <p>
     * Compiles a string expression and returns it as an {@link Evaluator} instance.
     * </p>
     *
     * @param expression expression to compile
     * @return evaluator instance
     * @see #buildExpression(String, ClassLoader, Properties)
     */
    default Evaluator buildExpression(String expression) {
        return buildExpression(expression, getRuntime().getClassLoader(), getRuntime().getConfiguration());
    }

    /**
     * <p>
     * Compiles a string expression and returns it as an {@link Evaluator} instance.
     * </p>
     *
     * @param expression expression to compile
     * @param classLoader classloader
     * @param properties optional properties for compiler
     * @return evaluator instance
     */
    default Evaluator buildExpression(String expression, ClassLoader classLoader, Properties properties) {
        try {
            ExpressionResolver resolver = getRuntime().getExpressionResolver();
            return resolver.buildExpression(expression, this, getRuntime().getImports().get(RuleScope.BOTH, RuleScope.LHS), classLoader, properties);
        } catch (CompilationException e) {
            throw new IllegalArgumentException("Unable to compile expression '" + expression + "'", e);
        }
    }

    default FieldReference[] resolve(String[] args) {
        return getRuntime().resolveFieldReferences(args, this);
    }
}
