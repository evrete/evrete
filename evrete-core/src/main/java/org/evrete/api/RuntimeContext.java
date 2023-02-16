package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;

import java.util.Comparator;

/**
 * @param <C> context type parameter
 */
public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<C>, FluentEnvironment<C>, EvaluatorsContext {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    RuleBuilder<C> newRule(String name);

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
