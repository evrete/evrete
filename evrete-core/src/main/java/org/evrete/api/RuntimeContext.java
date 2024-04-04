package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.util.CompilationException;
import org.evrete.util.TypeResolverWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * The RuntimeContext interface represents the context in which the rules are executed.
 * Both stateful and stateless sessions, as well as {@link Knowledge} instances, extend this interface.
 *
 * @param <C> the type of the implementing class or interface
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
     * especially for large rule sets. See the new {@link #builder()} method for details
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
     * Returns an instance of {@link RuleSetBuilder} for building and appending rules to the current context.
     * </p>
     * <p>
     * Builder <strong>MUST</strong> be terminated with the {@link RuleSetBuilder#build()} call for changes to take effect.
     * </p>
     * <p> Usage BEFORE 3.1.00:</p>
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute();
     * </code>
     * </pre>
     * Usage AFTER 3.1.00:
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .builder()       // !! important
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute()
     *      .build();        // !! important
     * </code>
     * </pre>
     *
     * @return new instance of RuleSetBuilder.
     * @since 3.1.00
     */
    RuleSetBuilder<C> builder();


    /**
     * @param expression literal condition and its context
     * @return new evaluator instance
     * @throws CompilationException if the expression failed to compile
     * @see #compile(Collection)
     */
    default LiteralEvaluator compile(LiteralExpression expression) throws CompilationException {
        return compile(Collections.singletonList(expression)).iterator().next();
    }

    /**
     * <p>
     * A convenience method for compiling literal expressions.
     * </p>
     *
     * @param expressions literal conditions and their context
     * @return compiled literal conditions
     * @throws CompilationException if the expression failed to compile
     */
    Collection<LiteralEvaluator> compile(Collection<LiteralExpression> expressions) throws CompilationException;

    /**
     * Wraps the provided TypeResolver with a TypeResolverWrapper instance.
     *
     * @param wrapper the TypeResolverWrapper instance used to wrap the TypeResolver
     * @deprecated This class is deprecated and will be removed in future releases.
     * Use the {@link RuntimeContext#configureTypes(Consumer)} method instead to configure the context's types and their fields.
     */
    @Deprecated
    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode activationMode);

    ExpressionResolver getExpressionResolver();

    /**
     * Configures the {@link TypeResolver} by applying the given action.
     *
     * @param action the action to configure the TypeResolver
     * @return this context
     */
    C configureTypes(Consumer<TypeResolver> action);

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
