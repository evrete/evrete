package org.evrete.runtime;

import org.evrete.api.Events;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.EventBus;

import java.util.concurrent.ExecutorService;

/**
 * Base class for {@link org.evrete.KnowledgeService} with runtime specific data
 */
public abstract class AbstractKnowledgeService implements EventBus {
    private final EventMessageBus messageBus;
    private final ExecutorService executor;

    public AbstractKnowledgeService(ExecutorService executor) {
        this.messageBus = new EventMessageBus(executor);
        this.executor = executor;
    }

    protected EventMessageBus getMessageBus() {
        return messageBus;
    }

    @Override
    public <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass) {
        return messageBus.getPublisher(eventClass);
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public final ExecutorService getExecutor() {
        return executor;
    }

}
