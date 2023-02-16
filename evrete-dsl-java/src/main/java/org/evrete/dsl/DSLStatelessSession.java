package org.evrete.dsl;

import org.evrete.api.FactHandle;
import org.evrete.api.StatelessSession;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DSLStatelessSession extends AbstractDSLSession<StatelessSession> implements StatelessSession {


    DSLStatelessSession(StatelessSession delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
        super(delegate, meta, fieldDeclarations, rules, classInstance);
    }

    @Override
    public void fire(BiConsumer<FactHandle, Object> consumer) {
        delegate.fire(consumer);
    }

    @Override
    public Void fire() {
        return delegate.fire();
    }

    @Override
    public void fire(Consumer<Object> consumer) {
        delegate.fire(consumer);
    }

    @Override
    public <T> void fire(String type, Consumer<T> consumer) {
        delegate.fire(type, consumer);
    }

    @Override
    public <T> void fire(Class<T> type, Consumer<T> consumer) {
        delegate.fire(type, consumer);
    }
}
