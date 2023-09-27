package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;

public interface Knowledge extends RuleSetContext<Knowledge, RuleDescriptor> {
    Collection<RuleSession<?>> getSessions();

    /**
     * @return new stateful session
     */
    StatefulSession newStatefulSession();

    default StatefulSession newStatefulSession(ActivationMode mode) {
        return newStatefulSession().setActivationMode(mode);
    }

    /**
     * @return new stateless session
     */
    StatelessSession newStatelessSession();

    default StatelessSession newStatelessSession(ActivationMode mode) {
        return newStatelessSession().setActivationMode(mode);
    }

    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }
}
