package org.evrete.api.events;

import org.evrete.api.RuleSession;

/**
 * Represents an event that occurs immediately after {@link RuleSession#fire()} is called.
 *
 * @see ContextEvent
 */
public interface SessionFireEvent extends ContextEvent {

    /**
     * Returns the {@link RuleSession} associated with this event.
     *
     * @return the session instance.
     */
    RuleSession<?> getSession();
}
