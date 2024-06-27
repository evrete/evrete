package org.evrete.runtime;

import org.evrete.api.Copyable;
import org.evrete.api.Events;
import org.evrete.api.events.*;
import org.evrete.util.BroadcastingPublisher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class EventMessageBus implements Copyable<EventMessageBus> {

    private final Executor executor;

    private final Map<Class<?>, Handler<?>> handlers;

    public EventMessageBus(Executor executor) {
        this.executor = executor;
        this.handlers = new HashMap<>();
        register(KnowledgeCreatedEvent.class, new KnowledgeCreatedEventHandler(executor));
        register(SessionCreatedEvent.class, new SessionCreatedEventHandler(executor));
        register(SessionClosedEvent.class, new SessionClosedEventHandler(executor));
        register(SessionFireEvent.class, new SessionFireEventHandler(executor));
        register(EnvironmentChangeEvent.class, new EnvironmentChangeEventHandler(executor));
    }

    private EventMessageBus(EventMessageBus parent) {
        this.executor = parent.executor;

        Map<Class<?>, Handler<?>> copy = new HashMap<>();
        parent.handlers.forEach((aClass, handler) -> copy.put(aClass, handler.copyOf()));
        this.handlers = copy;
    }


    private <E extends ContextEvent> void register(Class<E> type, Handler<E> handler) {
        this.handlers.put(type, handler);
    }

    Map<Class<?>, Handler<?>> getHandlers() {
        return handlers;
    }


    public <E extends ContextEvent> void broadcast(Class<E> type, E event) {
        getHandler(type).broadcast(event);
    }

    public <E extends ContextEvent> Events.Subscription subscribe(Class<E> eventClass, boolean async, Consumer<E> listener) {
        return getHandler(eventClass).subscribe(async, listener);
    }

    public <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass) {
        return getHandler(eventClass);
    }

    @SuppressWarnings("unchecked")
    private <E extends ContextEvent> Handler<E> getHandler(Class<E> eventClass) {
        Handler<E> handler = (Handler<E>) handlers.get(eventClass);
        if(handler == null) {
            throw new IllegalArgumentException("No broadcast publisher found for event type " + eventClass);
        } else {
            return handler;
        }
    }

    @Override
    public synchronized EventMessageBus copyOf() {
        return new EventMessageBus(this);
    }

    abstract static class Handler<E extends ContextEvent> extends BroadcastingPublisher<E> implements Copyable<Handler<E>> {

        Handler(Executor executor) {
            super(executor);
        }

        protected Handler(Handler<E> other) {
            super(other);
        }
    }

    static class KnowledgeCreatedEventHandler extends Handler<KnowledgeCreatedEvent> {
        KnowledgeCreatedEventHandler(Executor executor) {
            super(executor);
        }

        KnowledgeCreatedEventHandler(Handler<KnowledgeCreatedEvent> other) {
            super(other);
        }

        @Override
        public Handler<KnowledgeCreatedEvent> copyOf() {
            return new KnowledgeCreatedEventHandler(this);
        }
    }

    static class SessionCreatedEventHandler extends Handler<SessionCreatedEvent> {

        SessionCreatedEventHandler(Executor executor) {
            super(executor);
        }

        SessionCreatedEventHandler(SessionCreatedEventHandler other) {
            super(other);
        }

        @Override
        public Handler<SessionCreatedEvent> copyOf() {
            return new SessionCreatedEventHandler(this);
        }
    }

    static class SessionClosedEventHandler extends Handler<SessionClosedEvent> {

        SessionClosedEventHandler(Executor executor) {
            super(executor);
        }

        SessionClosedEventHandler(SessionClosedEventHandler other) {
            super(other);
        }

        @Override
        public Handler<SessionClosedEvent> copyOf() {
            return new SessionClosedEventHandler(this);
        }
    }

    static class SessionFireEventHandler extends Handler<SessionFireEvent> {

        SessionFireEventHandler(Executor executor) {
            super(executor);
        }

        SessionFireEventHandler(SessionFireEventHandler other) {
            super(other);
        }

        @Override
        public Handler<SessionFireEvent> copyOf() {
            return new SessionFireEventHandler(this);
        }
    }

    static class EnvironmentChangeEventHandler extends Handler<EnvironmentChangeEvent> {

        EnvironmentChangeEventHandler(Executor executor) {
            super(executor);
        }

        EnvironmentChangeEventHandler(EnvironmentChangeEventHandler other) {
            super(other);
        }

        @Override
        public Handler<EnvironmentChangeEvent> copyOf() {
            return new EnvironmentChangeEventHandler(this);
        }
    }

}
