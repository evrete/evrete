package org.evrete.api;

import java.util.Collection;

/**
 * Knowledge is a preprocessed ruleset. Although the engine allows direct creation of rule sessions,
 * having a prepared ruleset enables faster instantiation of sessions. Building knowledge instances
 * especially makes sense if the rules use literal conditions and therefore require a very CPU-intensive
 * compilation stage.
 */
public interface Knowledge extends RuleSetContext<Knowledge, RuleDescriptor> {
    /**
     * Retrieves a collection of active RuleSessions associated with the Knowledge.
     *
     * @return a collection of RuleSessions
     */
    Collection<RuleSession<?>> getSessions();

    /**
     * @return new stateful session
     */
    StatefulSession newStatefulSession();

    /**
     * Creates a new stateful session with the specified activation mode.
     *
     * @param mode the activation mode to set for the session
     * @return a new stateful session instance
     */
    default StatefulSession newStatefulSession(ActivationMode mode) {
        return newStatefulSession().setActivationMode(mode);
    }

    /**
     * @return new stateless session
     */
    StatelessSession newStatelessSession();

    /**
     * Creates a new {@link StatelessSession} with the specified {@link ActivationMode}.
     *
     * @param mode the activation mode to set for the session
     * @return a new stateless session instance with the specified activation mode
     */
    default StatelessSession newStatelessSession(ActivationMode mode) {
        return newStatelessSession().setActivationMode(mode);
    }

    /**
     * Sets the Activation Manager for the Knowledge instance.
     *
     * @param factory the class of the Activation Manager
     * @param <A>     the type of Activation Manager
     * @return the updated Knowledge instance
     */
    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }
}
