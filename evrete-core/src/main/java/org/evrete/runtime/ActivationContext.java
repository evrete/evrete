package org.evrete.runtime;

import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final AbstractKnowledgeSession<?> session;

    ActivationContext(AbstractKnowledgeSession<?> session) {
        this.session = session;
    }

    public AbstractKnowledgeSession<?> getSession() {
        return session;
    }

    int incrementFireCount() {
        return this.activationCount.getAndIncrement();
    }
}
