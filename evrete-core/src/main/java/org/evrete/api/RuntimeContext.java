package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.runtime.RuntimeListeners;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.structure.RuleDescriptor;

public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners {

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
