package org.evrete.runtime;

import java.util.concurrent.atomic.AtomicInteger;

public class FireContext {
    private final AtomicInteger fireCount = new AtomicInteger(0);
    private final StatefulSessionImpl session;

    FireContext(StatefulSessionImpl session) {
        this.session = session;
    }

    public StatefulSessionImpl getSession() {
        return session;
    }

    int incrementFireCount() {
        return this.fireCount.getAndIncrement();
    }
}
