package org.evrete.util;

import org.evrete.api.*;

import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class SessionWrapper<S extends RuleSession<S>> extends RuntimeContextWrapper<S> implements RuleSession<S> {
    protected SessionWrapper(S delegate) {
        super(delegate);
    }

    @Override
    public FactHandle insert(Object fact) {
        return delegate.insert(fact);
    }

    @Override
    public FactHandle insert(String type, Object fact) {
        return delegate.insert(type, fact);
    }

    @Override
    public ActivationManager getActivationManager() {
        return delegate.getActivationManager();
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setActivationManager(ActivationManager activationManager) {
        delegate.setActivationManager(activationManager);
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setExecutionPredicate(BooleanSupplier criteria) {
        delegate.setExecutionPredicate(criteria);
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S addEventListener(SessionLifecycleListener listener) {
        delegate.addEventListener(listener);
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S removeEventListener(SessionLifecycleListener listener) {
        delegate.removeEventListener(listener);
        return (S) this;
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
