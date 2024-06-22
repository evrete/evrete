package org.evrete.collections;

import org.evrete.api.annotations.NonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LongKeyMap<T> implements Iterable<T> {
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private static final float SHRINK_FACTOR = 0.25f;

    private static class Entry<T> {
        final long key;
        T value;
        Entry<T> next;

        Entry(long key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<T>[] table;
    private int size;
    private int threshold;
    private int shrinkThreshold;
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public LongKeyMap() {
        table = (Entry<T>[]) new Entry[INITIAL_CAPACITY];
        threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
        shrinkThreshold = (int) (INITIAL_CAPACITY * SHRINK_FACTOR);
    }

    /**
     * A shallow copy constructor
     * @param other the source map
     */
    public LongKeyMap(final LongKeyMap<T> other) {
        synchronized (other.lock) {
            table = other.table.clone();
            threshold = other.threshold;
            shrinkThreshold = other.shrinkThreshold;
            size = other.size;
        }
    }


    private int hash(long key) {
        return Integer.hashCode((int)key) & (table.length - 1);
    }

    public T put(long key, T value) {
        synchronized (lock) {
            int index = hash(key);
            Entry<T> entry = table[index];
            Entry<T> prev = null;

            while (entry != null) {
                if (entry.key == key) {
                    T oldValue = entry.value;
                    entry.value = value; // Update value if key exists
                    return oldValue;
                }
                prev = entry;
                entry = entry.next;
            }

            if (prev == null) {
                table[index] = new Entry<>(key, value); // Add new entry if chain is empty
            } else {
                prev.next = new Entry<>(key, value); // Add new entry to the chain
            }

            if (++size > threshold) {
                resize(table.length * 2);
            }

            return null;
        }
    }

    public void clear() {
        synchronized (lock) {
            @SuppressWarnings("unchecked")
            Entry<T>[] newTable = (Entry<T>[]) new Entry[INITIAL_CAPACITY];
            table = newTable;
            size = 0;
            threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
            shrinkThreshold = (int) (INITIAL_CAPACITY * SHRINK_FACTOR);
        }
    }

    private Stream<Entry<T>> entries() {
        return Arrays.stream(table)
                .filter(Objects::nonNull)
                .flatMap(entry -> {
                    Stream.Builder<Entry<T>> builder = Stream.builder();
                    while (entry != null) {
                        builder.accept(entry);
                        entry = entry.next;
                    }
                    return builder.build();
                });
    }

    public Stream<T> values() {
        return entries().map(entry -> entry.value);
    }

    public Stream<Long> keys() {
        return entries().map(entry -> entry.key);
    }

    public T computeIfAbsent(long key, Supplier<T> supplier) {
        T result = get(key);
        if(result == null) {
            synchronized (lock) {
                result = get(key);
                if(result == null) {
                    result = supplier.get();
                    put(key,result);
                }
            }
        }
        return result;
    }


    public T get(long key) {
        int index = hash(key);
        Entry<T> entry = table[index];

        while (entry != null) {
            if (entry.key == key) {
                return entry.value;
            }
            entry = entry.next;
        }
        return null;
    }

    public synchronized T remove(long key) {
        synchronized (lock) {
            int index = hash(key);
            Entry<T> entry = table[index];
            Entry<T> prev = null;

            while (entry != null) {
                if (entry.key == key) {
                    if (prev == null) {
                        table[index] = entry.next; // Remove first entry in chain
                    } else {
                        prev.next = entry.next; // Remove middle or last entry in chain
                    }
                    size--;
                    T ret = entry.value;
                    if (size < shrinkThreshold && table.length > INITIAL_CAPACITY) {
                        resize(table.length / 2);
                    }

                    return ret;
                }
                prev = entry;
                entry = entry.next;
            }

            return null; // Key not found
        }
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new Iterator<>() {
            int bucketIndex = 0;
            Entry<T> currentEntry = null;

            @Override
            public boolean hasNext() {
                if (currentEntry != null && currentEntry.next != null) {
                    return true;
                }
                while (bucketIndex < table.length) {
                    if (table[bucketIndex] != null) {
                        return true;
                    }
                    bucketIndex++;
                }
                return false;
            }

            @Override
            public T next() {
                if (currentEntry != null && currentEntry.next != null) {
                    currentEntry = currentEntry.next;
                } else {
                    while (bucketIndex < table.length && table[bucketIndex] == null) {
                        bucketIndex++;
                    }
                    if (bucketIndex >= table.length) {
                        throw new NoSuchElementException();
                    }
                    currentEntry = table[bucketIndex];
                    bucketIndex++;
                }
                return currentEntry.value;
            }
        };
    }


    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Entry<T>[] oldTable = table;
        table = (Entry<T>[]) new Entry[newCapacity];
        threshold = (int) (newCapacity * LOAD_FACTOR);
        shrinkThreshold = (int) (newCapacity * SHRINK_FACTOR);
        size = 0;

        for (Entry<T> entry : oldTable) {
            while (entry != null) {
                put(entry.key, entry.value);
                entry = entry.next;
            }
        }
    }

    public int size() {
        return size;
    }

    public static void main(String[] args) {
        LongKeyMap<String> map = new LongKeyMap<>();
        System.out.println(map.put(1, "one"));   // Output: null
        System.out.println(map.put(2, "two"));   // Output: null
        System.out.println(map.put(3, "three")); // Output: null
        System.out.println(map.put(1, "uno"));   // Output: one

        System.out.println(map.get(1)); // Output: uno
        System.out.println(map.get(2)); // Output: two
        System.out.println(map.get(3)); // Output: three

        System.out.println(map.remove(2)); // Output: two
        System.out.println(map.get(2));    // Output: null

        // Test shrinking
        for (int i = 4; i <= 20; i++) {
            map.put(i, "value" + i);
        }
        for (int i = 4; i <= 20; i++) {
            map.remove(i);
        }
        System.out.println("Size after removals: " + map.size()); // Size should be less than the shrink threshold
        System.out.println("Capacity after removals: " + map.table.length); // Capacity should be shrunk if below threshold

    }
}
