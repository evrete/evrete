package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Comparator;

/**
 * @param <C> context type parameter
 */
public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<C>, FluentEnvironment<C>, EvaluatorsContext {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    /**
     * Creates a new rule with the provided name.
     *
     * @param name rule name
     * @return a new rule builder
     * @deprecated this method is deprecated in favor of a more efficient way of building rules,
     * especially for large rule sets. See {@link #builder()} for details
     */
    @Deprecated
    RuleBuilder<C> newRule(String name);

    /**
     * Creates a new unnamed rule.
     *
     * @return a new rule builder
     * @deprecated this method is deprecated in favor of a more efficient way of building rules,
     * especially for large rule sets. See {@link #builder()} for details
     */
    @Deprecated
    RuleBuilder<C> newRule();


    /**
     * <p>
     *     Returns an instance of {@link RuleSetBuilder} for building and appending rules to the current context.
     * </p>
     * <p>
     *     Builder <strong>MUST</strong> be terminated with the {@link RuleSetBuilder#build()} call for changes to take effect.
     * </p>
     *
     * @return new instance of RuleSetBuilder.
     */
    RuleSetBuilder<C> builder();


    /**
     * <p>
     * A convenience wrapper for compiling literal conditions.
     * </p>
     *
     * @param expression literal condition and its context
     * @return new evaluator instance
     * @throws CompilationException if the expression failed to compile
     */
    LiteralEvaluator compile(LiteralExpression expression) throws CompilationException;

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode activationMode);

    ExpressionResolver getExpressionResolver();

    ClassLoader getClassLoader();

    /**
     * <p>
     * Sets new parent classloader for this context's internal classloader.
     * </p>
     *
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
