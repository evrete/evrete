package org.evrete.runtime;

import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final StatefulSessionImpl session;

    ActivationContext(StatefulSessionImpl session) {
        this.session = session;
    }

    public StatefulSessionImpl getSession() {
        return session;
    }


    int incrementFireCount() {
        return this.activationCount.getAndIncrement();
    }
}
