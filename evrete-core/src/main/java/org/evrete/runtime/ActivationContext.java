package org.evrete.runtime;

import org.evrete.util.NextIntSupplier;

class ActivationContext {
    private final NextIntSupplier activationCount = new NextIntSupplier();


    int incrementFireCount() {
        return this.activationCount.next();
    }
}
