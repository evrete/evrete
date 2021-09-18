package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.FactHandle;
import org.evrete.api.StatefulSession;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;
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

    public <T> StatefulSession forEachFact(String type, Consumer<T> consumer) {
        forEachFactInner(type, consumer);
        return this;
    }

    @Override
    public StatefulSession forEachFact(BiConsumer<FactHandle, Object> consumer) {
        forEachFactInner(consumer);
        return this;
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
    public StatefulSession fire() {
        fireInner();
        return this;
    }

    @Override
    public void clear() {
        clearInner();
    }

    @Override
    public final StatefulSession update(FactHandle handle, Object newValue) {
        updateInner(handle, newValue);
        return this;
    }

    @Override
    public final StatefulSession delete(FactHandle handle) {
        deleteInner(handle);
        return this;
    }

    @Override
    public <T> Future<T> fireAsync(final T result) {
        return getExecutor().submit(this::fire, result);
    }


}
