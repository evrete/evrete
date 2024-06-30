package org.evrete.runtime.events;

import org.evrete.api.events.TimedEvent;

import java.time.Instant;

public abstract class AbstractTimedEvent implements TimedEvent {
    private final Instant startTime;
    private final Instant endTime;

    public AbstractTimedEvent(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public AbstractTimedEvent(Instant startTime) {
        this(startTime, Instant.now());
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Instant getEndTime() {
        return endTime;
    }
}
