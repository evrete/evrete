package org.evrete.runtime;

import org.evrete.api.events.Events;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.EventBus;

import java.util.concurrent.ExecutorService;

/**
 * Base class for {@link org.evrete.KnowledgeService} with runtime specific data
 */
public abstract class AbstractKnowledgeService implements EventBus {
    private final EventMessageBus messageBus;
    private final ExecutorService executor;
    private final Events.Subscriptions serviceSubscriptions;

    public AbstractKnowledgeService(ExecutorService executor) {
        this.serviceSubscriptions = new Events.Subscriptions();
        this.messageBus = new EventMessageBus(executor);
        this.executor = executor;
    }

    protected EventMessageBus getMessageBus() {
        return messageBus;
    }

    /**
     * Retrieves the shared {@link Events.Subscriptions} collection at the service level.
     * Subscriptions added to this collection will be automatically cancelled when the {@link #shutdown()}
     * method is called. This centralized storage can be used by those who prefer not to manually manage
     * the lifecycle of their subscriptions, ensuring that resources are freed up for garbage collection
     * when no longer needed.
     *
     * @return the shared {@link Events.Subscriptions} instance at the service level.
     */
    public Events.Subscriptions getServiceSubscriptions() {
        return serviceSubscriptions;
    }

    @Override
    public <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass) {
        return messageBus.getPublisher(eventClass);
    }

    public void shutdown() {
        this.executor.shutdown();
        this.serviceSubscriptions.cancel();
    }

    public final ExecutorService getExecutor() {
        return executor;
    }

}
