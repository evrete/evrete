package org.evrete.collections;

import org.evrete.api.ReIterator;

import java.util.Objects;

public final class JoinReIterator<V> implements ReIterator<V> {
    private final ReIterator<V>[] iterators;
    private ReIterator<V> current;
    private int currentIndex;

    private JoinReIterator(ReIterator<V>[] iterators) {
        this.iterators = iterators;
        this.currentIndex = 0;
        this.current = iterators[0];
    }

    @SafeVarargs
    public static <V, Z extends ReIterator<V>> ReIterator<V> of(Z... iterators) {
        Objects.requireNonNull(iterators);
        return iterators.length == 0 ? ReIterator.emptyIterator() : new JoinReIterator<>(iterators);
    }

    @Override
    public long reset() {
        long size = 0L;
        for (ReIterator<V> it : iterators) {
            size += it.reset();
        }
        this.currentIndex = 0;
        this.current = iterators[currentIndex];
        return size;
    }

    @Override
    public void remove() {
        current.remove();
    }

    @Override
    public boolean hasNext() {
        if (current.hasNext()) {
            return true;
        } else {
            if (++currentIndex == iterators.length) {
                return false;
            } else {
                current = iterators[currentIndex];
                return hasNext();
            }
        }
    }

    @Override
    public V next() {
        return current.next();
    }
}
