package org.evrete.collections;

import org.evrete.api.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A simple hash map implementation using open addressing with linear probing.
 *
 * <p>Main Operations:</p>
 * <ul>
 *     <li>{@code T put(long key, T value)} - Inserts a key-value pair and returns previous value.</li>
 *     <li>{@code T get(long key)} - Retrieves the value for a key.</li>
 *     <li>{@code T remove(long key)} - Removes a key-value pair.</li>
 *     <li>{@code int size()} - Returns the number of key-value pairs.</li>
 * </ul>
 *
 * @param <T> the type of values maintained by this map
 */
public class LongObjectHashMap<T> {

    private static final int DEFAULT_CAPACITY = 16;
    private Entry[] table;
    private int size;
    private final int initialSize;

    public LongObjectHashMap() {
        this(DEFAULT_CAPACITY);
    }

    public LongObjectHashMap(int capacity) {
        int adjusted = nextPowerOfTwo(capacity);
        table = new Entry[adjusted];
        initialSize = adjusted;
        size = 0;
    }

    public LongObjectHashMap(LongObjectHashMap<T> other) {
        table = other.table.clone();
        size = other.size;
        initialSize = other.initialSize;
    }

    private int hash(long key) {
        return ((int) key) & (table.length - 1);
    }

    int tableSize() {
        return table.length;
    }

    public synchronized void clear() {
        this.size = 0;
        if(table.length == initialSize) {
            Arrays.fill(table, null);
        } else {
            table = new Entry[initialSize];
        }
    }

    @SuppressWarnings("unchecked")
    public Stream<T> values() {
        return Arrays.stream(this.table)
                .filter(entry -> entry != null && entry.isActive)
                .map(entry -> (T) entry.value);
    }

    private int nextHash(int hash) {
        return (hash + 1) & (table.length - 1);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T put(long key, T value) {
        return (T) this.putInner(key, value);
    }

    @Nullable
    public T put(int key, T value) {
        return this.put((long) key, value);
    }

    private synchronized Object putInner(long key, Object value) {
        if (size >= table.length / 2) {
            resize();
        }

        int hash = hash(key);
        Entry entry;
        while ((entry = table[hash]) != null && entry.isActive) {
            if (entry.key == key) {
                Object oldValue = entry.value;
                entry.value = value;  // Update existing value
                return oldValue;
            }
            hash = nextHash(hash);
        }
        table[hash] = new Entry(key, value);
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T get(long key) {
        int hash = hash(key);
        Entry entry;
        while ((entry = table[hash]) != null) {
            if (entry.isActive && entry.key == key) {
                return (T) entry.value;
            }
            hash = nextHash(hash);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public synchronized T remove(long key) {
        int hash = hash(key);
        Entry entry;
        while ((entry = table[hash]) != null) {
            if (entry.isActive && entry.key == key) {
                entry.isActive = false;
                size--;
                return (T) entry.value;
            }
            hash = nextHash(hash);
        }
        return null;
    }

    public int size() {
        return size;
    }

    private void resize() {
        Entry[] oldTable = table;
        table = new Entry[2 * oldTable.length];
        size = 0;

        for (Entry entry : oldTable) {
            if (entry != null && entry.isActive) {
                putInner(entry.key, entry.value);
            }
        }
    }

    public static int nextPowerOfTwo(int number) {
        if(number <= 0) {
            throw new IllegalArgumentException();
        }
        int highestOneBit = Integer.highestOneBit(number);
        if (number == highestOneBit) {
            return number;
        } else if (highestOneBit < (1 << 30)) {
            return highestOneBit << 1;
        } else {
            throw new IllegalStateException("Not enough memory");
        }
    }

    private static class Entry {
        long key;
        Object value;
        boolean isActive;

        Entry(long key, Object value) {
            this.key = key;
            this.value = value;
            this.isActive = true;
        }
    }

}
