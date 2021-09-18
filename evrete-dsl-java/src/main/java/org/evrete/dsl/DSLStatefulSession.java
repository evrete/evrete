package org.evrete.dsl;

import org.evrete.api.FactHandle;
import org.evrete.api.StatefulSession;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DSLStatefulSession extends AbstractDSLSession<StatefulSession> implements StatefulSession {

    DSLStatefulSession(StatefulSession delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
        super(delegate, meta, fieldDeclarations, rules, classInstance);
    }

    @Override
    public StatefulSession fire() {
        delegate.fire();
        return this;
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
    public StatefulSession update(FactHandle handle, Object newValue) {
        delegate.update(handle, newValue);
        return this;
    }

    @Override
    public StatefulSession delete(FactHandle handle) {
        delegate.delete(handle);
        return this;
    }

    @Override
    public <T> T getFact(FactHandle handle) {
        return delegate.getFact(handle);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public StatefulSession forEachFact(BiConsumer<FactHandle, Object> consumer) {
        delegate.forEachFact(consumer);
        return this;
    }

    @Override
    public <T> StatefulSession forEachFact(String type, Consumer<T> consumer) {
        delegate.forEachFact(type, consumer);
        return this;
    }
}
