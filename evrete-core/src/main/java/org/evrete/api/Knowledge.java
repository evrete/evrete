package org.evrete.api;

import java.util.Collection;

/**
 * Knowledge is a preprocessed ruleset. Although the engine allows direct creation of rule sessions,
 * having a prepared ruleset enables faster instantiation of sessions. Building knowledge instances
 * especially makes sense if the rules use literal conditions and therefore require a very CPU-intensive
 * compilation stage.
 */
public interface Knowledge extends RuleSetContext<Knowledge, RuleDescriptor> {
    Collection<RuleSession<?>> getSessions();

    /**
     * @return new stateful session
     */
    StatefulSession newStatefulSession();

    @SuppressWarnings("resource")
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
