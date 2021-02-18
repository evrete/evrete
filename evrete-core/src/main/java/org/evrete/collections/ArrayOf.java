package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * A simple array wrapper which allows for faster iteration and
 * array resize while keeping the same wrapper instance
 *
 * @param <T> type parameter
 */
//TODO !!!! create tests
public class ArrayOf<T> implements ReIterable<T> {
    private final static int NULL_INDEX = -1;
    //TODO !!! make private
    public T[] data;
    private int nonNullSize = 0;

    public ArrayOf(T[] data) {
        this.data = data;
        this.nonNullSize = computeSize();
    }

    public ArrayOf(ArrayOf<T> other) {
        this.data = Arrays.copyOf(other.data, other.data.length);
        this.nonNullSize = computeSize();
    }

    //TODO !!!! keep track of current size
    private int computeSize() {
        int size = 0;
        for (T o : data) {
            if (o != null) size++;
        }
        return size;
    }

    public ArrayOf(Class<T> type) {
        this(CollectionUtils.array(type, 0));
    }

    public void append(T element) {
        int ret = this.data.length;
        this.data = Arrays.copyOf(this.data, ret + 1);
        this.data[ret] = element;
    }

    public void set(int index, T element) {
        if (index >= data.length) {
            this.data = Arrays.copyOf(this.data, index + 1);
        }
        this.data[index] = element;
    }

    public void forEach(Consumer<? super T> consumer) {
        for (T obj : data) {
            if (obj != null) {
                consumer.accept(obj);
            }
        }
    }

    public boolean isEmptyAt(int index) {
        if (index >= this.data.length) return true;// No such index
        return this.data[index] == null;
    }

    public T computeIfAbsent(int idx, IntFunction<T> supplier) {
        T obj = get(idx);
        if (obj == null) {
            obj = supplier.apply(idx);
            set(idx, obj);
        }

        return obj;
    }

    public T getChecked(int i) {
        T obj = get(i);
        if (obj == null) {
            throw new IllegalStateException("No data initialized for " + i);
        }
        return obj;
    }

    public T get(int i) {
        if (i >= data.length || i < 0) {
            return null;
        } else {
            return data[i];
        }
    }

    public void clear() {
        Arrays.fill(data, null);
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    public ReIterator<T> iterator() {
        return new It();
    }

    private class It implements ReIterator<T> {

        private int cursor;

        It() {
            init();
        }

        private void init() {
            cursor = findNonNullIndex(0);
        }

        private int findNonNullIndex(int startInclusive) {
            int current = startInclusive;
            while (current < data.length) {
                if (data[current] != null) return current;
                current++;
            }
            return NULL_INDEX;
        }

        @Override
        public long reset() {
            init();
            return computeSize();
        }

        @Override
        public boolean hasNext() {
            return this.cursor != NULL_INDEX;
        }

        @Override
        public T next() {
            T obj = data[this.cursor];
            this.cursor = findNonNullIndex(this.cursor + 1);
            return obj;
        }
    }
}
