package org.evrete.collections;

import org.evrete.api.annotations.Nullable;
import org.evrete.util.Indexed;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A simple array-backed synchronized map implementation that deals with {@link Indexed} keys.
 * It uses a sparse array as storage, so the {@link #values()} and {@link #forEach(Consumer)} methods
 * perform best with auto-incremented indices.
 *
 * @param <K> the type of the access key
 * @param <V> the type of the stored value
 */
public class ArrayMap<K extends Indexed, V> {
    private static final int DEFAULT_INITIAL_SIZE = 16;
    private Object[] data;

    public ArrayMap() {
        this.data = new Object[DEFAULT_INITIAL_SIZE];
    }

    public ArrayMap(int initialCapacity) {
        this.data = new Object[Math.max(initialCapacity, DEFAULT_INITIAL_SIZE)];
    }

    public synchronized void put(K key, V value) {
        int idx = key.getIndex();
        put(idx, value);
    }

    private void put(int idx, V value) {
        if(idx >= data.length) {
            data = Arrays.copyOf(data,  Math.max(idx * 2, DEFAULT_INITIAL_SIZE));
        }
        data[idx] = value;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        int idx = key.getIndex();

        V result = nullable(idx);
        if(result == null) {
            synchronized (this) {
                result = nullable(idx);
                if(result == null) {
                    result = mappingFunction.apply(key);
                    put(idx, result);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private V nullable(int idx) {
        if(idx >= data.length) {
            return null;
        } else {
            return (V) data[idx];
        }
    }

    public V getChecked(K key) {
        return Objects.requireNonNull(get(key));
    }

    @Nullable
    public V get(K key) {
        return nullable(key.getIndex());
    }

    public void clear() {
        Arrays.fill(data, null);
    }

    @SuppressWarnings("unchecked")
    public Stream<V> values() {
        return Arrays.stream(this.data).filter(Objects::nonNull).map(o -> (V) o);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        for (Object o : data) {
            if (o != null) {
                action.accept((V) o);
            }
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }
}
