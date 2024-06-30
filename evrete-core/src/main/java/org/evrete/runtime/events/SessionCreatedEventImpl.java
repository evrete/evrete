package org.evrete.runtime.events;

import org.evrete.api.RuleSession;
import org.evrete.api.events.SessionCreatedEvent;

import java.time.Instant;

public class SessionCreatedEventImpl extends AbstractTimedEvent implements SessionCreatedEvent {
    private final RuleSession<?> session;

    public SessionCreatedEventImpl(Instant startTime, RuleSession<?> context) {
        super(startTime);
        this.session = context;
    }

    @Override
    public RuleSession<?> getSession() {
        return session;
    }
}
