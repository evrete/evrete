package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.runtime.RuleDescriptor;

import java.util.Comparator;
import java.util.List;

public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<RuntimeContext<?>>, PropertyAccess<C> {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    boolean ruleExists(String name);

    List<RuleDescriptor> getRuleDescriptors();

    Kind getKind();

    RuleDescriptor compileRule(RuleBuilder<?> builder);

    void deployRule(RuleDescriptor descriptor);

    RuleBuilder<C> newRule(String name);

    RuleBuilder<C> newRule();

    RuntimeContext<?> getParentContext();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode agendaMode);

    ClassLoader getClassLoader();

    void setClassLoader(ClassLoader classLoader);

    Class<? extends ActivationManager> getActivationManagerFactory();

    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    void setActivationManagerFactory(String managerClass);

    TypeResolver getTypeResolver();

    Configuration getConfiguration();

    enum Kind {
        KNOWLEDGE, SESSION
    }
}
