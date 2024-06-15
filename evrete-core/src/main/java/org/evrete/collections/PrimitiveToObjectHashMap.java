//package org.evrete.collections;
//
//import org.evrete.api.annotations.Nullable;
//import org.evrete.util.CommonUtils;
//
//import java.util.Arrays;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Stream;
//
///**
// * A simple hash map implementation using open addressing with linear probing.
// *
// * <p>Main Operations:</p>
// * <ul>
// *     <li>{@code T put(int key, T value)} - Inserts a key-value pair and returns previous value.</li>
// *     <li>{@code T get(int key)} - Retrieves the value for a key.</li>
// *     <li>{@code T remove(int key)} - Removes a key-value pair.</li>
// *     <li>{@code int size()} - Returns the number of key-value pairs.</li>
// * </ul>
// *
// * @param <T> the type of values maintained by this map
// */
//public class PrimitiveToObjectHashMap<E extends PrimitiveToObjectHashMap.Entry, T> {
//
//    private static final int DEFAULT_CAPACITY = 16;
//    private E[] table;
//    private int size;
//
//    public PrimitiveToObjectHashMap(Class<E> entryClass) {
//        this(entryClass, DEFAULT_CAPACITY);
//    }
//
//    public PrimitiveToObjectHashMap(Class<E> entryClass, int capacity) {
//        table = CommonUtils.array(entryClass, nextPowerOfTwo(capacity));
//        size = 0;
//    }
//
//    public PrimitiveToObjectHashMap(PrimitiveToObjectHashMap<E, T> other) {
//        table = other.table.clone();
//        size = other.size;
//    }
//
//    private int hash(int key) {
//        return key & (table.length - 1);
//    }
//
//    int tableSize() {
//        return table.length;
//    }
//
//    public synchronized void clear() {
//        this.size = 0;
//        Arrays.fill(table, null);
//    }
//
//    @SuppressWarnings("unchecked")
//    public Stream<T> values() {
//        return Arrays.stream(this.table).filter(new Predicate<Entry>() {
//            @Override
//            public boolean test(Entry entry) {
//                return entry != null && entry.isActive;
//            }
//        }).map(new Function<Entry, T>() {
//            @Override
//            public T apply(Entry entry) {
//                return (T) entry.value;
//            }
//        });
//    }
//
//    private int nextHash(int hash) {
//        return (hash + 1) & (table.length - 1);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Nullable
//    public T put(int key, T value) {
//        return (T) this.putInner(key, value);
//    }
//
//    private synchronized Object putInner(int key, Object value) {
//        if (size >= table.length / 2) {
//            rehash();
//        }
//
//        int hash = hash(key);
//        Entry entry;
//        while ((entry = table[hash]) != null && entry.isActive) {
//            if (entry.key == key) {
//                Object oldValue = entry.value;
//                entry.value = value;  // Update existing value
//                return oldValue;
//            }
//            hash = nextHash(hash);
//        }
//        table[hash] = new Entry(key, value);
//        size++;
//        return null;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Nullable
//    public T get(int key) {
//        int hash = hash(key);
//        Entry entry;
//        while ((entry = table[hash]) != null) {
//            if (entry.isActive && entry.key == key) {
//                return (T) entry.value;
//            }
//            hash = nextHash(hash);
//        }
//        return null;
//    }
//
//    @SuppressWarnings("unchecked")
//    public synchronized T remove(int key) {
//        int hash = hash(key);
//        Entry entry;
//        while ((entry = table[hash]) != null) {
//            if (entry.isActive && entry.key == key) {
//                entry.isActive = false;
//                size--;
//                return (T) entry.value;
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
//    private void rehash() {
//        Entry[] oldTable = table;
//        table = new Entry[2 * oldTable.length];
//        size = 0;
//
//        for (Entry entry : oldTable) {
//            if (entry != null && entry.isActive) {
//                putInner(entry.key, entry.value);
//            }
//        }
//    }
//
//    public static int nextPowerOfTwo(int number) {
//        if(number <= 0) {
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
//    static class Entry {
//        int key;
//        Object value;
//        boolean isActive;
//
//        Entry(int key, Object value) {
//            this.key = key;
//            this.value = value;
//            this.isActive = true;
//        }
//    }
//
//}
