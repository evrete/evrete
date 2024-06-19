//package org.evrete.collections;
//
//import org.evrete.api.annotations.NonNull;
//import org.evrete.api.annotations.Nullable;
//
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.NoSuchElementException;
//import java.util.function.Function;
//import java.util.function.Supplier;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
///**
// * A simple linear hash map implementation using open addressing with linear probing.
// * This implementation conforms to {@link Iterable} and provides streams of its values.
// *
// * <p>Main Operations:</p>
// * <ul>
// *     <li>{@code T put(long key, T value)} - Inserts a key-value pair and returns the previous value.</li>
// *     <li>{@code T get(long key)} - Retrieves the value for a key.</li>
// *     <li>{@code T remove(long key)} - Removes a key-value pair.</li>
// *     <li>{@code int size()} - Returns the number of key-value pairs.</li>
// * </ul>
// *
// * @param <T> the type of values maintained by this map
// */
//public class LongObjectHashMap<T> implements Iterable<T> {
//
//    private static final int DEFAULT_CAPACITY = 16;
//    private Entry[] table;
//    private int size;
//    private int[] indices;
//    private int indicesWritePos;
//
//    private final int minSize;
//
//    public LongObjectHashMap() {
//        this(DEFAULT_CAPACITY);
//    }
//
//    public LongObjectHashMap(int capacity) {
//        int adjusted = nextPowerOfTwo(capacity);
//        this.table = new Entry[adjusted];
//        this.minSize = adjusted;
//        this.size = 0;
//        this.indices = new int[adjusted];
//        this.indicesWritePos = 0;
//    }
//
//    public LongObjectHashMap(LongObjectHashMap<T> other) {
//        this.table = other.table.clone();
//        this.size = other.size;
//        this.minSize = other.minSize;
//        this.indices = other.indices.clone();
//        this.indicesWritePos = other.indicesWritePos;
//    }
//
//    private int hash(long key) {
//        return ((int) key) & (table.length - 1);
//    }
//
//    int tableSize() {
//        return table.length;
//    }
//
//    @Override
//    public String toString() {
//        return "{" +
//                "size=" + size +
//                ", indicesWritePos=" + indicesWritePos +
//                '}';
//    }
//
//    public synchronized void clear() {
//        this.size = 0;
//        this.indicesWritePos = 0;
//        if (table.length == minSize) {
//            Arrays.fill(table, null);
//            Arrays.fill(indices, 0);
//        } else {
//            table = new Entry[minSize];
//            indices = new int[minSize];
//        }
//    }
//
//    private Stream<Entry> entries() {
//        return IntStream.range(0, indicesWritePos)
//                .mapToObj(value -> table[indices[value]])
//                .filter(entry -> entry != null && entry.isActive);
//    }
//
//
//
//    @SuppressWarnings("unchecked")
//    public Stream<T> values() {
//        return entries().map(entry -> (T) entry.value);
//    }
//
//    public Stream<Long> keys() {
//        return entries().map(entry -> entry.key);
//    }
//
//    private int nextHash(int hash) {
//        return (hash + 1) & (table.length - 1);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Nullable
//    public T put(long key, T value) {
//        Object result = this.putInner(key, value);
//        checkResize();
//        if (result == null) {
//            return null;
//        } else {
//            return (T) result;
//        }
//    }
//
//    @Nullable
//    public T put(int key, T value) {
//        return this.put((long) key, value);
//    }
//
//    private synchronized Object putInner(long key, Object value) {
//        int idx = hash(key);
//        Entry entry;
//        int counter = 0;
//        while ((entry = table[idx]) != null) {
//            if (entry.isActive && entry.key == key) {
//                Object oldValue = entry.value;
//                entry.value = value;  // Update existing value
//                return oldValue;
//            }
//            idx = nextHash(idx);
//            counter++;
//            if(counter > 10_000_000) {
//                throw new IllegalArgumentException();
//            }
//        }
//        this.table[idx] = new Entry(key, value);
//        this.indices[this.indicesWritePos++] = idx;
//        this.size++;
//        return null;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Nullable
//    public T get(long key) {
//        int idx = hash(key);
//        Entry entry;
//        while ((entry = table[idx]) != null) {
//            if (entry.isActive && entry.key == key) {
//                return (T) entry.value;
//            }
//            idx = nextHash(idx);
//        }
//        return null;
//    }
//
//    public T computeIfAbsent(long key, Supplier<T> supplier) {
//        T result = get(key);
//        if(result == null) {
//            synchronized (this) {
//                result = get(key);
//                if(result == null) {
//                    result = supplier.get();
//                    put(key,result);
//                }
//            }
//        }
//        return result;
//    }
//
//    @SuppressWarnings("unchecked")
//    public synchronized T remove(long key) {
//        int hash = hash(key);
//        Entry entry;
//        while ((entry = table[hash]) != null) {
//            if (entry.isActive && entry.key == key) {
//                entry.isActive = false;
//                size--;
//                T ret = (T) entry.value;
//                // Resize only if actually deleted
//                checkResize();
//                return ret;
//            }
//            hash = nextHash(hash);
//        }
//        return null;
//    }
//
//    public int size() {
//        return size;
//    }
//
//    private void checkResize() {
//        int deletes = indicesWritePos - size;
//        if (deletes > size) {
//            // Too many delete ops, resize is mandatory
//            resize(idealSize());
//        } else {
//            int idealSize = idealSize();
//            if (idealSize != this.table.length) {
//                resize(idealSize);
//            }
//        }
//    }
//
//    int idealSize() {
//        return Math.max(minSize, nextPowerOfTwo(this.size + 1) * 2);
//    }
//
//    private void resize(int newSize) {
//        Entry[] oldTable = this.table;
//        this.table = new Entry[newSize];
//        this.size = 0;
//        this.indices = new int[newSize];
//        this.indicesWritePos = 0;
//
//        int s = 0;
//        for (Entry entry : oldTable) {
//            if (entry != null && entry.isActive) {
//                s++;
//                putInner(entry.key, entry.value);
//            }
//        }
//        // TODO delete me
//        assert s == this.size : "Actual: " +s + " != " + this.size;
//    }
//
//    public static int nextPowerOfTwo(int number) {
//        if (number <= 0) {
//            throw new IllegalArgumentException();
//        }
//        int highestOneBit = Integer.highestOneBit(number);
//        if (number == highestOneBit) {
//            return number;
//        } else if (highestOneBit < (1 << 30)) {
//            return highestOneBit << 1;
//        } else {
//            throw new IllegalStateException("Not enough memory");
//        }
//    }
//
//    private static class Entry {
//        long key;
//        Object value;
//        boolean isActive;
//
//        Entry(long key, Object value) {
//            this.key = key;
//            this.value = value;
//            this.isActive = true;
//        }
//    }
//
//    @Override
//    @NonNull
//    public Iterator<T> iterator() {
//        return new It();
//    }
//
//
//    private class It implements Iterator<T> {
//        private int currentIndex;
//
//        private It() {
//            this.currentIndex = 0;
//            advanceToNextNonNull();
//        }
//
//        // Advances the currentIndex to the next non-null element
//        private void advanceToNextNonNull() {
//            Entry entry;
//            while (currentIndex < indices.length && ((entry = current()) == null || !entry.isActive)) {
//                currentIndex++;
//            }
//        }
//
//        private Entry current() {
//            return table[indices[currentIndex]];
//        }
//
//        @Override
//        public boolean hasNext() {
//            return currentIndex < indices.length;
//        }
//
//        @Override
//        @SuppressWarnings("unchecked")
//        public T next() {
//            if (hasNext()) {
//                T value = (T) current().value;
//                currentIndex++;
//                advanceToNextNonNull();
//                return value;
//            } else {
//                throw new NoSuchElementException();
//            }
//        }
//    }
//}
