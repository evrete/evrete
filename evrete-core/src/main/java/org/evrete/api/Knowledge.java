package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;

public interface Knowledge extends RuntimeContext<Knowledge>, RuleSet<RuleDescriptor> {
    Collection<RuleSession<?>> getSessions();

    StatefulSession createSession();

    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }
}
