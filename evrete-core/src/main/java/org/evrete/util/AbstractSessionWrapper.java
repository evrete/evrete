package org.evrete.util;

import org.evrete.api.*;

import java.util.function.BooleanSupplier;
import java.util.stream.Collector;

/**
 * This class provides an abstract implementation of the RuleSession interface, delegating the session's
 * methods to a provided delegate object.
 *
 * @param <S> the type of the {@link RuleSession} delegate object
 */
public abstract class AbstractSessionWrapper<S extends RuleSession<S>> extends RuntimeContextWrapper<S, S, RuntimeRule> implements RuleSession<S> {
    protected AbstractSessionWrapper(S delegate) {
        super(delegate);
    }

    @Override
    public ActivationManager getActivationManager() {
        return delegate.getActivationManager();
    }

    @Override
    public <T> Collector<T, ?, S> asCollector() {
        return new SessionCollector<>(self());
    }

    @Override
    public S setActivationManager(ActivationManager activationManager) {
        delegate.setActivationManager(activationManager);
        return self();
    }

    @Override
    public S setExecutionPredicate(BooleanSupplier criteria) {
        delegate.setExecutionPredicate(criteria);
        return self();
    }

    @Override
    public S addEventListener(SessionLifecycleListener listener) {
        delegate.addEventListener(listener);
        return self();
    }


    @Override
    public S removeEventListener(SessionLifecycleListener listener) {
        delegate.removeEventListener(listener);
        return self();
    }

    @Override
    public Knowledge getParentContext() {
        return delegate.getParentContext();
    }

    @Override
    public FactHandle insert0(Object fact, boolean resolveCollections) {
        return delegate.insert0(fact, resolveCollections);
    }

    @Override
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return delegate.insert0(type, fact, resolveCollections);
    }
}
