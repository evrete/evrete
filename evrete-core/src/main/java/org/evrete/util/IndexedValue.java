package org.evrete.util;

/**
 * <p>
 * Represents an object with an assigned unique index.
 * </p>
 * @param <T>
 */
public class IndexedValue<T> implements Indexed {
    private final int index;
    private final T value;

    public IndexedValue(int index, T value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexedValue<?> indexed = (IndexedValue<?>) o;
        return index == indexed.index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + index +
                ", value=" + value +
                '}';
    }
}
