package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A simple implementation of a linear probing hash table, devoid of fail-fast bells and whistles and similar features.
 * Contrary to Java's standard HashMap, this implementation can reduce its bucket table size when required,
 * thus preserving the true O(N) scan complexity.
 *
 * @param <E> Entry type
 */
public abstract class AbstractLinearHash<E> implements ReIterable<E> {
    private static final Logger LOGGER = Logger.getLogger(AbstractLinearHash.class.getName());
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 1 << 4;
    int size = 0;
    int deletes = 0;
    Entry[] data;
    int upperResizeBound;
    int lowerResizeBound;

    protected AbstractLinearHash() {
        this(MINIMUM_CAPACITY);
    }

    private AbstractLinearHash(int initialCapacity) {
        int capacity = tableSizeFor(initialCapacity);
        this.data = new Entry[capacity];
        this.setResizeBounds(capacity);
    }

    private static int findEmptyBin(int hash, int mask, Entry[] destination) {
        int pos = hash & mask, counter = 0;
        while (destination[pos] != null) {
            if (counter++ == destination.length) {
                throw new IllegalStateException("Low-level implementation error, please submit a bug.");
            } else {
                pos = (pos + 1) & mask;
            }
        }
        return pos;
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    private static int tableSizeFor(int dataSize) {
        int nextPowerOfTwo = Integer.highestOneBit(dataSize);
        final int cap;
        if (dataSize == nextPowerOfTwo) {
            cap = dataSize;
        } else {
            cap = nextPowerOfTwo << 1;
        }
        assert cap >= dataSize;
        if (cap > MAXIMUM_CAPACITY) throw new OutOfMemoryError();
        return Math.max(MINIMUM_CAPACITY, cap);
    }

    private <T> Entry findBinEntry(T key, int hash, BiPredicate<E, T> eqTest) {
        final int size = data.length, mask = size - 1;
        int pos = hash & mask, counter = 0;
        Entry found;
        while ((found = data[pos]) != null) {
            if (eqTest.test(found.cast(), key)) {
                return found;
            } else {
                if (counter++ == size) {
                    LOGGER.warning("Low-level implementation waring, please submit a bug.");
                    return null;
                } else {
                    pos = (pos + 1) & mask;
                }
            }
        }
        return null;
    }

    private <T> int findBinIndexForInsert(T key, int hash, BiPredicate<E, T> eqTest) {
        final int size = data.length, mask = size - 1;
        int pos = hash & mask, counter = 0;
        Entry found;
        while ((found = data[pos]) != null && !found.deleted) {
            if (eqTest.test(found.cast(), key)) {
                return pos;
            } else {
                if (counter++ == size) {
                    throw new IllegalStateException("Low-level implementation error, please submit a bug.");
                } else {
                    pos = (pos + 1) & mask;
                }
            }
        }
        return pos;
    }

    /**
     * Computes the value associated with the given key if it is absent in the hash table. If the value is absent, the supplied producer function is called to generate the value.
     * If the value is present, the supplied action function is called with the existing value as an argument.
     *
     * @param key            The key used to compute the value.
     * @param searchFunction A function to test if the existing value matches the key.
     * @param producer       A function to generate the value if it is absent.
     * @param action         A consumer function to perform an action on the existing value.
     * @param <K>            The type of the key.
     * @return true if the value was absent and added, false if the value was present.
     */
    public <K> boolean computeIfAbsent(K key, BiPredicate<E, K> searchFunction, Function<K, E> producer, Consumer<E> action) {
        this.resize();
        int hash = key.hashCode();
        int binIndex = this.findBinIndexForInsert(key, hash, searchFunction);
        E existing = this.get(binIndex);
        if (existing == null) {
            this.saveDirect(producer.apply(key), hash, binIndex);
            return true;
        } else {
            action.accept(existing);
            return false;
        }
    }

    /**
     * Inserts an element into the hash table if it does not already exist.
     *
     * @param key            The key used to compute the value.
     * @param searchFunction A function to test if the existing value matches the key.
     * @param producer       A function to generate the value if it is absent. The function accepts the key and it's computed hash
     * @param <K>            The type of the key.
     * @return The inserted element if it does not already exist, null otherwise.
     */
    public <K> E insertIfAbsent(K key, BiPredicate<E, K> searchFunction, ObjIntFunction<K, E> producer) {
        this.resize();
        int hash = key.hashCode();
        int binIndex = this.findBinIndexForInsert(key, hash, searchFunction);
        E existing = this.get(binIndex);
        if (existing == null) {
            E newValue = producer.apply(hash, key);
            this.saveDirect(newValue, hash, binIndex);
            return newValue;
        } else {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key.
     * If the value is absent, the supplied producer function is called to generate the value and update the hash table.
     *
     * @param <K>            The type of the key.
     * @param key            The key used to compute the value.
     * @param searchFunction A function to test if the existing value matches the key.
     * @param producer       A function to generate the value if it is absent.
     * @return The computed value.
     */
    public <K> E computeIfAbsent(K key, BiPredicate<E, K> searchFunction, Function<K, E> producer) {
        this.resize();
        int hash = key.hashCode();
        int binIndex = this.findBinIndexForInsert(key, hash, searchFunction);
        E result = this.get(binIndex);
        if (result == null) {
            result = producer.apply(key);
            this.saveDirect(result, hash, binIndex);
        }
        return result;
    }

    public <K> boolean remove(K key, BiPredicate<E, K> searchFunction) {
        this.resize();
        int hash = key.hashCode();
        Entry entry = this.findBinEntry(key, hash, searchFunction);
        return deleteEntry(entry);
    }

    public <K> boolean contains(K key, BiPredicate<E, K> searchFunction) {
        int hash = key.hashCode();
        final int size = data.length, mask = size - 1;
        int pos = hash & mask, counter = 0;
        Entry found;
        while ((found = data[pos]) != null) {
            if (searchFunction.test(found.cast(), key)) {
                Entry entry = data[pos];
                return !entry.deleted;
            } else {
                if (counter++ == size) {
                    return false;
                } else {
                    pos = (pos + 1) & mask;
                }
            }
        }
        return false;
    }

    public <K> E get(K key, BiPredicate<E, K> searchFunction) {
        int hash = key.hashCode();
        Entry entry = this.findBinEntry(key, hash, searchFunction);
        return this.get(entry);
    }

    private E get(int pos) {
        Entry entry = data[pos];
        return get(entry);
    }

    private E get(Entry entry) {
        return entry == null || entry.deleted ? null : entry.cast();
    }

    public final <K> E add(K key, BiPredicate<E, K> equalsTest, @NonNull E element) {
        resize();
        int hash = key.hashCode();
        int pos = findBinIndexForInsert(key, hash, equalsTest);
        return saveDirect(element, hash, pos);
    }

    private E saveDirect(@NonNull E element, int hash, int pos) {
        Entry existing = data[pos];
        if (existing == null) {
            Entry newEntry = new Entry(element, false, hash);
            data[pos] = newEntry;
            size++;
            return null;
        } else {
            if (existing.deleted) {
                existing.value = element;
                existing.hash = hash;
                existing.deleted = false;
                size++;
                deletes--;
                return null;
            } else {
                E ret = existing.cast();
                existing.value = element;
                existing.hash = hash;
                return ret;
            }
        }
    }


    public final int size() {
        return size;
    }

    final void deleteEntries(Predicate<E> predicate) {
        forEachDataEntry((e, i) -> {
            if (predicate.test(e)) {
                deleteEntry(i);
            }
        });
        resize();
    }

    /**
     * Adds all elements from a given source object to the current object. If elements with the same key exist, it uses a combine function to resolve the conflict.
     * <p>
     * The combine function's first argument is an element from this collection (and it may be null), and the second one is an element from the source collection (non-null).
     * The result of this function must produce a null or a value with the same hash code as the original keys. If the result is null, then the associated local value (if any) will be deleted.
     *
     * @param source          the source collection to add elements from.
     * @param combineFunction a BinaryOperator function which acts on two inputs of type E (the element type in
     *                        the collection) and produces an output of the same type.
     */
    public void addAll(AbstractLinearHash<E> source, BinaryOperator<E> combineFunction, BiPredicate<E, E> matchFunction) {
        //TODO create new table here (or continue as-is if size allows)
        source.forEachInnerEntry(otherEntry -> {
            resize();
            int hash = otherEntry.hash;
            E otherValue = otherEntry.cast();
            int binIndex = findBinIndexForInsert(otherValue, hash, matchFunction);
            @Nullable E result = combineFunction.apply(get(binIndex), otherValue);

            if (result == null) {
                deleteEntry(binIndex);
            } else {
                saveDirect(result, hash, binIndex);
            }
        });
    }

    void forEachInnerEntry(Consumer<Entry> consumer) {
        for (Entry o : this.data) {
            if (o != null && !o.deleted) {
                consumer.accept(o);
            }
        }
    }

    public void forEachDataEntry(Consumer<E> consumer) {
        for (Entry o : this.data) {
            if (o != null && !o.deleted) {
                consumer.accept(o.cast());
            }
        }
    }

    private void forEachDataEntry(ObjIntConsumer<E> consumer) {
        int idx;
        E obj;
        for (idx = 0; idx < data.length; idx++) {
            if ((obj = get(idx)) != null) {
                consumer.accept(obj, idx);
            }
        }
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        forEachDataEntry(k -> joiner.add(k.toString()));
        return joiner.toString();
    }

    public void clear() {
        CollectionUtils.systemFill(this.data, null);
        this.size = 0;
    }

    private void deleteEntry(int pos) {
        Entry entry = data[pos];
        deleteEntry(entry);
    }

    private boolean deleteEntry(Entry entry) {
        if (entry == null) {
            return false;
        } else if (entry.deleted) {
            return false;
        } else {
            entry.deleted = true;
            size--;
            deletes++;
            return true;
        }
    }

    @Override
    @NonNull
    public ReIterator<E> iterator() {
        return new It();
    }

    public Stream<E> stream() {
        return Arrays.stream(data, 0, data.length)
                .filter(Objects::nonNull)
                .map(Entry::cast)
                ;
    }

    private void setResizeBounds(int tableSize) {
        this.upperResizeBound = (int) (tableSize * 0.75f);
        this.lowerResizeBound = (int) (tableSize * 0.25f);
    }

    public void resize() {
        int newTableSize = -1;

        if (size >= upperResizeBound) {
            // Resize up
            newTableSize = data.length * 2;
        } else if (size <= lowerResizeBound) {
            // Resize down
            int shrunkSize = Math.max(data.length / 2, MINIMUM_CAPACITY);
            if (shrunkSize != this.data.length) {
                newTableSize = shrunkSize;
            }
        }

        if (newTableSize > 0) {
            rebuild(newTableSize);
        }

    }

    private void rebuild(int newArrSize) {
        Entry[] newData = new Entry[newArrSize];
        int mask = newArrSize - 1;
        for (Entry o : this.data) {
            if (o != null && !o.deleted) {
                int pos = findEmptyBin(o.hash, mask, newData);
                newData[pos] = o;
            }
        }
        this.data = newData;
        this.deletes = 0;
        this.setResizeBounds(newArrSize);
    }

    static class Entry {
        @NonNull
        Object value;
        boolean deleted;
        int hash;

        public Entry(@NonNull Object value, boolean deleted, int hash) {
            this.value = value;
            this.deleted = deleted;
            this.hash = hash;
        }

        @SuppressWarnings("unchecked")
        <T> T cast() {
            return (T) value;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "{" +
                    "value=" + value +
                    ", del=" + deleted +
                    ", hash=" + hash +
                    '}';
        }
    }

    private final class It implements ReIterator<E> {
        int pos = 0;
        int nextIndex;
        int currentIndex = -1;

        private It() {
            // Initial advance
            nextIndex = computeNextIndex();
        }

        @Override
        public long reset() {
            pos = 0;
            nextIndex = computeNextIndex();
            return size;
        }

        @Override
        public boolean hasNext() {
            return nextIndex >= 0;
        }

        private int computeNextIndex() {
            Entry entry;
            while (pos < data.length) {
                if ((entry = data[pos]) == null || entry.deleted) {
                    pos++;
                } else {
                    return pos;
                }
            }
            return -1;
        }

        @Override
        public E next() {
            if (nextIndex < 0) {
                throw new NoSuchElementException();
            } else {
                pos++;
                currentIndex = nextIndex;
                nextIndex = computeNextIndex();
                return data[currentIndex].cast();
            }
        }

        @Override
        public void remove() {
            if (currentIndex < 0) {
                throw new NoSuchElementException();
            } else {
                deleteEntry(data[currentIndex]);
            }
        }
    }
}
