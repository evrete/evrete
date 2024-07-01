package org.evrete.api.events;

import org.evrete.api.RuleSession;

/**
 * Represents an event that occurs when a new {@link RuleSession} is created
 *
 * @see ContextEvent
 */
public interface SessionCreatedEvent extends TimedEvent, ContextEvent {


    /**
     * Returns the created session
     * @return the created session
     */
    RuleSession<?> getSession();

}
