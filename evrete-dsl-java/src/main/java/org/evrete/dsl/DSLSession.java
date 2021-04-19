package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.util.SessionWrapper;

import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

class DSLSession extends SessionWrapper {
    private final Listeners listeners;

    DSLSession(StatefulSession delegate, Listeners listeners) {
        super(delegate);
        this.listeners = listeners;
    }

    @Override
    public void fire() {
        listeners.fire(Phase.FIRE, this);
        super.fire();
    }

    @Override
    public <T> Future<T> fireAsync(T result) {
        listeners.fire(Phase.FIRE, this);
        return super.fireAsync(result);
    }

    @Override
    public void close() {
        listeners.fire(Phase.CLOSE, this);
        super.close();
    }

    @Override
    public StatefulSession set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @Override
    public RuntimeContext<?> addImport(RuleScope scope, String imp) {
        super.addImport(scope, imp);
        return this;
    }

    @Override
    public StatefulSession setActivationMode(ActivationMode activationMode) {
        super.setActivationMode(activationMode);
        return this;
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        super.setActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        super.setFireCriteria(fireCriteria);
        return this;
    }
}
