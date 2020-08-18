package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.runtime.RuntimeListeners;

import java.util.Comparator;

public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners {
    Comparator<Rule> SALIENCE_COMPARATOR = (o1, o2) -> -1 * Integer.compare(o1.getSalience(), o2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    boolean ruleExists(String name);

    Kind getKind();

    RuleDescriptor compileRule(RuleBuilder<?> builder);

    RuntimeRule deployRule(RuleDescriptor descriptor);

    RuleBuilder<C> newRule(String name);

    RuleBuilder<C> newRule();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    RuntimeListeners getListeners();

    TypeResolver getTypeResolver();

    RuleDescriptor getRuleDescriptor(String name);

    default RuleDescriptor getRuleDescriptor(Named named) {
        return getRuleDescriptor(named.getName());
    }

    Configuration getConfiguration();

    enum Kind {
        KNOWLEDGE, SESSION
    }
}
