package org.evrete.collections;

public class Indexed<T> {
    private final int index;
    private final T value;

    public Indexed(int index, T value) {
        this.index = index;
        this.value = value;
    }
}
