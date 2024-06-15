package org.evrete.runtime.events;

public interface EngineEventPublisher<T> {

    void publish(T event);
}
