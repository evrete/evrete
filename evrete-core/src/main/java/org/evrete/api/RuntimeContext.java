package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.util.compiler.CompilationException;

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
     * <p>
     * A convenience wrapper for compiling literal conditions.
     * </p>
     *
     * @param expression string condition
     * @param resolver   named type resolver
     * @return new evaluator instance
     * @throws CompilationException if the expression failed to compile
     */
    default Evaluator compile(String expression, NamedType.Resolver resolver) throws CompilationException {
        return getExpressionResolver().buildExpression(expression, resolver);
    }

    RuleBuilder<C> newRule();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode activationMode);

    ExpressionResolver getExpressionResolver();

    ClassLoader getClassLoader();

    void setClassLoader(ClassLoader classLoader);

    KnowledgeService getService();

    Class<? extends ActivationManager> getActivationManagerFactory();

    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    void setActivationManagerFactory(String managerClass);

    TypeResolver getTypeResolver();

    Configuration getConfiguration();
}
