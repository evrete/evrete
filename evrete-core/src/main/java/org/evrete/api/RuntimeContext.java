package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.runtime.RuntimeListeners;

import java.util.Comparator;

public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<RuntimeContext<?>>, PropertyAccess<C> {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    boolean ruleExists(String name);

    Kind getKind();

    RuleDescriptor compileRule(RuleBuilder<?> builder);

    RuntimeRule deployRule(RuleDescriptor descriptor);

    RuleBuilder<C> newRule(String name);

    RuleBuilder<C> newRule();

    RuntimeContext<?> getParentContext();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    RuntimeListeners getListeners();

    C setActivationMode(ActivationMode agendaMode);

    ClassLoader getClassLoader();

    void setClassLoader(ClassLoader classLoader);

    Class<? extends ActivationManager> getActivationManagerFactory();

    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    void setActivationManagerFactory(String managerClass);

    TypeResolver getTypeResolver();

    RuleDescriptor getRuleDescriptor(String name);

    Configuration getConfiguration();

    enum Kind {
        KNOWLEDGE, SESSION
    }
}
