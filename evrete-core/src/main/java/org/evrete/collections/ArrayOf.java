package org.evrete.collections;

import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;

/**
 * A simple array wrapper which allows for faster iteration and
 * array resize while keeping the same wrapper instance
 *
 * @param <T> type parameter
 */
public class ArrayOf<T> {
    private T[] data;

    public ArrayOf(T[] data) {
        this.data = data;
    }

    public ArrayOf(ArrayOf<T> other) {
        this.data = Arrays.copyOf(other.data, other.data.length);
    }

    public ArrayOf(Class<T> type) {
        this(CollectionUtils.array(type, 0));
    }

    public int length() {
        return data.length;
    }

    public T[] getData() {
        return data;
    }

    public void append(T element) {
        int index = this.data.length;
        this.data = Arrays.copyOf(this.data, index + 1);
        this.set(index, element);
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

    public void forEach(ObjIntConsumer<? super T> consumer) {
        for (int i = 0; i < this.data.length; i++) {
            T obj = this.data[i];
            if (obj != null) {
                consumer.accept(obj, i);
            }
        }
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
}
