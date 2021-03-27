package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;

@SuppressWarnings("unused")
public interface Knowledge extends RuntimeContext<Knowledge>, RuleSet<RuleDescriptor> {
    Collection<KnowledgeSession<?>> getSessions();

    StatefulSession createSession();

    @Override
    Knowledge addImport(RuleScope scope, String imp);

    @Override
    Knowledge addImport(RuleScope scope, Class<?> type);

    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }
}
