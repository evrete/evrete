package org.evrete.collections;

import org.evrete.api.Copyable;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class IndexedMap<T, K, V extends Indexed<T>> implements Copyable<IndexedMap<T, K, V>> {
    private final ConcurrentHashMap<K, V> keyMap;
    private Object[] array;
    private final Function<T, K> keyFunction;
    private final ObjIntFunction<T, V> createFunction;

    public IndexedMap(Function<T, K> keyFunction, ObjIntFunction<T, V> createFunction) {
        this.keyMap = new ConcurrentHashMap<>();
        this.keyFunction = keyFunction;
        this.createFunction = createFunction;
        this.array = new Object[0];
    }

    private IndexedMap(IndexedMap<T, K, V> other) {
        this.keyMap = new ConcurrentHashMap<>(other.keyMap);
        this.array = other.array.clone();
        this.keyFunction = other.keyFunction;
        this.createFunction = other.createFunction;
    }

    @Override
    public IndexedMap<T, K, V> copyOf() {
        return new IndexedMap<>(this);
    }

    public V getCreate(T arg) {
        K key = keyFunction.apply(arg);
        return keyMap.computeIfAbsent(key, k -> {
            int newIndex = array.length;
            V newValue = createFunction.apply(newIndex, arg);
            Object[] newArray = new Object[newIndex + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[newIndex] = newValue;
            array = newArray;
            return newValue;
        });
    }

    @SuppressWarnings("unchecked")
    public V get(int index) {
        if (index < 0 || index >= array.length) {
            throw new NoSuchElementException();
        } else {
            return (V) array[index];
        }
    }
}
