package org.evrete.benchmarks.helper;

public interface IterableSet<T> extends IterableCollection<T> {

    boolean contains(T element);

    boolean remove(T element);
}
