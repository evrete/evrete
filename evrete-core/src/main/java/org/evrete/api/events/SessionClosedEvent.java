package org.evrete.api.events;

import org.evrete.api.RuleSession;

/**
 * Represents an event that occurs when a {@link RuleSession} is closed
 *
 * @see ContextEvent
 */
public interface SessionClosedEvent extends ContextEvent {


    RuleSession<?> getSession();

}
