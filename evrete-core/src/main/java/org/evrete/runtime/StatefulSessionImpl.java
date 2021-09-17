package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.FactHandle;
import org.evrete.api.StatefulSession;

import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class StatefulSessionImpl extends AbstractRuleSession<StatefulSession> implements StatefulSession {

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }


    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        applyActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        applyFireCriteria(fireCriteria);
        return this;
    }

    public <T> void forEachFact(String type, Consumer<T> consumer) {
        forEachFactInner(type, consumer);
    }

    @Override
    public <T> T getFact(FactHandle handle) {
        return getFactInner(handle);
    }

    @Override
    public void close() {
        closeInner();
    }

    @Override
    public void fire() {
        fireInner();
    }

    @Override
    public void clear() {
        clearInner();
    }

    @Override
    public final void update(FactHandle handle, Object newValue) {
        updateInner(handle, newValue);
    }

    @Override
    public final void delete(FactHandle handle) {
        deleteInner(handle);
    }

    @Override
    public <T> Future<T> fireAsync(final T result) {
        return getExecutor().submit(this::fire, result);
    }


}
