package org.evrete.api.events;

import java.time.Instant;

public interface TimedEvent extends ContextEvent {
    Instant getStartTime();

    Instant getEndTime();
}
