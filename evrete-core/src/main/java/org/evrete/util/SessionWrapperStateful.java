package org.evrete.util;

import org.evrete.api.*;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SessionWrapperStateful extends RuntimeContextWrapper<StatefulSession> implements StatefulSession {

    protected SessionWrapperStateful(StatefulSession delegate) {
        super(delegate);
    }

    @Override
    public ActivationManager getActivationManager() {
        return delegate.getActivationManager();
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        return delegate.setActivationManager(activationManager);
    }

    @Override
    public void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        delegate.forEachFact(consumer);
    }

    @Override
    public <T> void forEachFact(String type, Consumer<T> consumer) {
        delegate.forEachFact(type, consumer);
    }

    @Override
    public RuntimeContext<?> getParentContext() {
        return delegate.getParentContext();
    }

    @Override
    public void fire() {
        delegate.fire();
    }

    @Override
    public <T> Future<T> fireAsync(T result) {
        return delegate.fireAsync(result);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void clear() {
        delegate.clear();
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
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        return delegate.setFireCriteria(fireCriteria);
    }

    @Override
    public RuntimeRule getRule(String name) {
        return delegate.getRule(name);
    }

    @Override
    public FactHandle insert(Object fact) {
        return delegate.insert(fact);
    }

    @Override
    public <T> T getFact(FactHandle handle) {
        return delegate.getFact(handle);
    }

    @Override
    public FactHandle insert(String type, Object fact) {
        return delegate.insert(type, fact);
    }

    @Override
    public void update(FactHandle handle, Object newValue) {
        delegate.update(handle, newValue);
    }

    @Override
    public void delete(FactHandle handle) {
        delegate.delete(handle);
    }
}
