package org.evrete.api.events;

import org.evrete.api.Knowledge;

/**
 * Represents an event that occurs when a new {@link Knowledge} is created
 *
 * @see ContextEvent
 */
public interface KnowledgeCreatedEvent extends ContextEvent {


    Knowledge getKnowledge();

}
