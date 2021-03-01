package org.evrete.runtime;

import java.util.concurrent.atomic.AtomicInteger;

class ActivationContext {
    private final AtomicInteger activationCount = new AtomicInteger(0);


    int incrementFireCount() {
        return this.activationCount.getAndIncrement();
    }
}
