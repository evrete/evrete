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
    public void setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler handler) {
        delegate.setRuleBuilderExceptionHandler(handler);
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
    public List<RuntimeRule> getRules() {
        return delegate.getRules();
    }

    public void addRule(RuleBuilder<?> builder) {
        delegate.addRule(builder);
    }

    @Override
    public RuntimeRule getRule(String name) {
        return delegate.getRule(name);
    }

    @Override
    public FieldReference[] resolveFieldReferences(String[] args, NamedType.Resolver typeMapper) {
        return delegate.resolveFieldReferences(args, typeMapper);
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
