package org.evrete.util;

import org.evrete.api.*;

import java.util.stream.Collector;
import java.util.stream.Stream;

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

    @Override
    public Stream<MapEntry<FactHandle, Object>> streamFactEntries() {
        return delegate.streamFactEntries();
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(String type) {
        return delegate.streamFactEntries(type);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(Class<T> type) {
        return delegate.streamFactEntries(type);
    }

    @Override
    public <T> T getFact(FactHandle handle) {
        return delegate.getFact(handle);
    }

    @Override
    public boolean delete(FactHandle handle) {
        return delegate.delete(handle);
    }

    @Override
    public void update(FactHandle handle, Object newValue) {
        delegate.update(handle, newValue);
    }
}
