package org.evrete.util;

import java.util.function.Supplier;

public class LazyInstance<T> {
    private final Supplier<T> supplier;
    private T instance;

    public LazyInstance(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if(instance == null) {
            synchronized (supplier) {
                if(instance == null) {
                    instance = supplier.get();
                }
            }
        }
        return instance;
    }

    public void set(T obj) {
        this.instance = obj;
    }
}
