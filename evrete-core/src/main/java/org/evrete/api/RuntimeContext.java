package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Comparator;

/**
 * @param <C> context type parameter
 */
public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<C>, FluentEnvironment<C>, EvaluatorsContext {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    RuleBuilder<C> newRule(String name);

    /**
     * @deprecated use {@link #compile(LiteralExpression)} instead
     */
    @Deprecated
    default Evaluator compile(String expression, NamedType.Resolver resolver) throws CompilationException {
        return getExpressionResolver().buildExpression(expression, resolver);
    }

    /**
     * <p>
     * A convenience wrapper for compiling literal conditions.
     * </p>
     *
     * @param expression literal condition and its context
     * @return new evaluator instance
     * @throws CompilationException if the expression failed to compile
     */
    default LiteralEvaluator compile(LiteralExpression expression) throws CompilationException {
        return getExpressionResolver().buildExpression(expression);
    }

    RuleBuilder<C> newRule();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode activationMode);

    ExpressionResolver getExpressionResolver();

    ClassLoader getClassLoader();

    /**
     * <p>
     *     Sets new parent classloader for this context's internal classloader.
     * </p>
     * @param classLoader this context's new parent classloader
     */
    void setClassLoader(ClassLoader classLoader);

    KnowledgeService getService();

    Class<? extends ActivationManager> getActivationManagerFactory();

    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    void setActivationManagerFactory(String managerClass);

    TypeResolver getTypeResolver();

    Configuration getConfiguration();

    JavaSourceCompiler getSourceCompiler();

}
