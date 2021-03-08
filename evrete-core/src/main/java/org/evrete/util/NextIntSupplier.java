package org.evrete.util;

import org.evrete.api.Copyable;

public class NextIntSupplier implements Copyable<NextIntSupplier> {
    private int counter = 0;

    public int next() {
        return counter++;
    }

    public int get() {
        return counter;
    }

    @Override
    public NextIntSupplier copyOf() {
        NextIntSupplier copy = new NextIntSupplier();
        copy.counter = this.counter;
        return copy;
    }
}
