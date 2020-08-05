package org.evrete.util;

import org.evrete.api.Copyable;

import java.util.concurrent.atomic.AtomicInteger;

public class NextIntSupplier implements Copyable<NextIntSupplier> {
    private final AtomicInteger counter = new AtomicInteger(0);

    public int next() {
        return counter.getAndIncrement();
    }

    public int get() {
        return counter.get();
    }

    @Override
    public NextIntSupplier copyOf() {
        NextIntSupplier copy = new NextIntSupplier();
        copy.counter.set(this.counter.get());
        return copy;
    }
}
