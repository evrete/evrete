package org.evrete.collections;

import org.evrete.api.EachRunnable;
import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;

public class NestedReIterator<T> implements EachRunnable {
    private final ReIterator<T>[] iterators;
    private final T[] state;
    private final int lastIndex;

    @SuppressWarnings("unchecked")
    public NestedReIterator(T[] state) {
        this.iterators = (ReIterator<T>[]) new ReIterator[state.length];
        this.state = state;
        this.lastIndex = state.length - 1;
    }

    public <I extends ReIterable<T>> void setIterables(I[] data) {
        for (int i = 0; i < data.length; i++) {
            set(i, data[i].iterator());
        }
    }

    public long reset() {
        long l = 1;
        for (ReIterator<T> it : iterators) {
            l *= it.reset();
        }
        return l;
    }

    public <I extends ReIterator<T>> void setIterators(I[] data) {
        for (int i = 0; i < data.length; i++) {
            set(i, data[i]);
        }
    }

    private void set(int index, ReIterator<T> iterator) {
        this.iterators[index] = iterator;
    }

    @Override
    public void runForEach(Runnable r) {
        runForEach(0, r);
    }

    private void runForEach(int index, Runnable r) {
        ReIterator<T> it = iterators[index];
        if (it.reset() == 0) return;
        if (index == lastIndex) {
            while (it.hasNext()) {
                state[index] = it.next();
                r.run();
            }
        } else {
            while (it.hasNext()) {
                state[index] = it.next();
                runForEach(index + 1, r);
            }
        }
    }
}
