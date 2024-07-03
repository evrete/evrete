package org.evrete.runtime.events;

import org.evrete.api.Knowledge;
import org.evrete.api.events.KnowledgeCreatedEvent;

import java.time.Instant;

public class KnowledgeCreatedEventImpl extends AbstractTimedEvent implements KnowledgeCreatedEvent {
    private final Knowledge knowledge;

    public KnowledgeCreatedEventImpl(Instant startTime, Knowledge context) {
        super(startTime);
        this.knowledge = context;
    }

    @Override
    public Knowledge getKnowledge() {
        return knowledge;
    }
}
