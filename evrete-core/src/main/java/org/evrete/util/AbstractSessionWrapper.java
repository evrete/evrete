package org.evrete.util;

import org.evrete.api.*;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collector;

public abstract class AbstractSessionWrapper<S extends RuleSession<S>> extends RuntimeContextWrapper<S> implements RuleSession<S> {
    protected AbstractSessionWrapper(S delegate) {
        super(delegate);
    }

    @Override
    public FactHandle insert(Object fact) {
        return delegate.insert(fact);
    }

    @Override
    public FactHandle insertAs(String type, Object fact) {
        return delegate.insertAs(type, fact);
    }

    @Override
    public ActivationManager getActivationManager() {
        return delegate.getActivationManager();
    }

    protected abstract S thisInstance();

    @Override
    public <T> Collector<T, ?, S> asCollector() {
        return new SessionCollector<>(thisInstance());
    }

    @Override
    public S setActivationManager(ActivationManager activationManager) {
        delegate.setActivationManager(activationManager);
        return thisInstance();
    }

    @Override
    public S setExecutionPredicate(BooleanSupplier criteria) {
        delegate.setExecutionPredicate(criteria);
        return thisInstance();
    }

    @Override
    public S addEventListener(SessionLifecycleListener listener) {
        delegate.addEventListener(listener);
        return thisInstance();
    }

    @Override
    public S removeEventListener(SessionLifecycleListener listener) {
        delegate.removeEventListener(listener);
        return thisInstance();
    }

    @Override
    public Knowledge getParentContext() {
        return delegate.getParentContext();
    }

    @Override
    public List<RuntimeRule> getRules() {
        return delegate.getRules();
    }

    @Override
    public RuntimeRule compileRule(RuleBuilder<?> builder) {
        return delegate.compileRule(builder);
    }

    @Override
    public RuntimeRule getRule(String name) {
        return delegate.getRule(name);
    }
}
