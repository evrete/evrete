package org.evrete.api.events;

import org.evrete.api.RuleSession;

/**
 * Represents an event that occurs when a new {@link RuleSession} is created
 *
 * @see ContextEvent
 */
public interface SessionCreatedEvent extends ContextEvent {


    RuleSession<?> getSession();

}
