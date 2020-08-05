package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.runtime.RuntimeListeners;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.structure.RuleDescriptor;

public interface RuntimeContext<C extends RuntimeContext<C, R>, R> extends Listeners {

    boolean ruleExists(String name);

    Kind getKind();

    RuleDescriptor compileRule(RuleBuilder<C> builder);

    RuntimeRule deployRule(RuleDescriptor descriptor);

    RuleBuilder<C> newRule(String name);

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    RuntimeListeners getListeners();

    RuleBuilder<C> newRule();

    RuleBuilder<C> getRuleBuilder(String name);

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
