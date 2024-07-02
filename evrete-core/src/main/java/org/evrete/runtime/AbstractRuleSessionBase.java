package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.api.events.SessionClosedEvent;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.GroupingReteMemory;
import org.evrete.api.spi.MemoryFactory;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.util.SessionCollector;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * <p>
 * Base session class with common methods
 * </p>
 *
 * @param <S> session type parameter
 */
public abstract class AbstractRuleSessionBase<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S>, MemoryStreaming {
    private final KnowledgeRuntime knowledge;
    ActivationManager activationManager;
    private volatile boolean active = true;
    protected final boolean warnUnknownTypes;
    private final MemoryFactory<DefaultFactHandle> memoryFactory;

    AbstractRuleSessionBase(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.memoryFactory = getService().getMemoryFactoryProvider().instance(this, DefaultFactHandle.class);
        this.knowledge = knowledge;
        this.activationManager = newActivationManager();
        this.warnUnknownTypes = getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
    }

    protected abstract S thisInstance();

    public abstract SessionMemory getMemory();

    MemoryFactory<DefaultFactHandle> getMemoryFactory() {
        return memoryFactory;
    }

    @Override
    public final S setActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
        return thisInstance();
    }


    @Override
    public final ActivationManager getActivationManager() {
        return activationManager;
    }


    @Override
    final void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    @Override
    public final KnowledgeRuntime getParentContext() {
        return knowledge;
    }


    @Override
    final public <T> Collector<T, ?, S> asCollector() {
        return new SessionCollector<>(thisInstance());
    }

    final void closeInner() {
        synchronized (this) {
            invalidateSession();
            broadcast(SessionClosedEvent.class, () -> AbstractRuleSessionBase.this);
            knowledge.close(this);
        }
    }

    private void invalidateSession() {
        this.active = false;
        //this.getMemory().clear();
    }

    public Stream<Map.Entry<FactHandle, Object>> streamFactEntries(boolean closeSession) {
        return streamMapper(getMemory().streamFactEntries(), closeSession);
    }

    public <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(String type, boolean closeSession) {
        return streamMapper(getMemory().streamFactEntries(type), closeSession);
    }

    public <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(Class<T> type, boolean closeSession) {
        return streamMapper(getMemory().streamFactEntries(type), closeSession);
    }

    private <T> Stream<T> streamMapper(Stream<T> stream, boolean closeSession) {
        if (closeSession) {
            return stream.onClose(this::closeInner);
        } else {
            return stream;
        }
    }

    public FactStorage<DefaultFactHandle, FactHolder> newTypeFactStorage() {
        return getMemoryFactory().newFactStorage(FactHolder.class);
    }

    public ValueIndexer<FactFieldValues> newFieldValuesIndexer() {
        return getMemoryFactory().newValueIndexed(FactFieldValues.class);
    }

    public GroupingReteMemory<DefaultFactHandle> newAlphaMemoryStorage() {
        return getMemoryFactory().newGroupedFactStorage(DefaultFactHandle.class);
    }

}
