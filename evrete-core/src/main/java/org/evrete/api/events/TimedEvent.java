package org.evrete.api.events;

import java.time.Instant;

/**
 * The TimedEvent interface represents a {@link ContextEvent} that has a start and end time.
 */
public interface TimedEvent {

    /**
     * Returns the start time of the event.
     *
     * @return the start time as an Instant object.
     */
    Instant getStartTime();

    /**
     * Returns the end time of the event.
     *
     * @return the end time as an Instant object.
     */
    Instant getEndTime();
}
