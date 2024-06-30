package org.evrete.api.events;

import org.evrete.api.Knowledge;

/**
 * Represents an event that occurs when a new instance of {@link Knowledge} is created.
 *
 * @see ContextEvent
 */
public interface KnowledgeCreatedEvent extends TimedEvent {


    /**
     * Gets the newly created {@link Knowledge} instance associated with this event.
     *
     * @return the newly created {@link Knowledge} instance.
     */
    Knowledge getKnowledge();

}
