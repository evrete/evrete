package org.evrete.util;

import org.evrete.api.Copyable;

public class NextIntSupplier implements Copyable<NextIntSupplier> {
    private int counter;

    private NextIntSupplier(int counter) {
        this.counter = counter;
    }

    public NextIntSupplier() {
        this(0);
    }

    public int next() {
        return counter++;
    }

    public void set(int value) {
        this.counter = value;
    }

    public int get() {
        return counter;
    }

    @Override
    public NextIntSupplier copyOf() {
        return new NextIntSupplier(this.counter);
    }
}
