package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;

public interface Knowledge extends RuntimeContext<Knowledge>, RuleSet<RuleDescriptor> {
    Collection<RuleSession<?>> getSessions();

    /**
     * <p>Deprecated since 2.0.5, use {@link #newStatefulSession()} instead.</p>
     *
     * @return new stateful session
     */
    @Deprecated()
    default StatefulSession createSession() {
        return newStatefulSession();
    }

    /**
     * @return new stateful session
     */
    StatefulSession newStatefulSession();

    /**
     * @return new stateless session
     */
    StatelessSession newStatelessSession();

    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }
}
