package org.evrete.collections;

import org.evrete.api.IntObjectPredicate;
import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.CollectionUtils;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

// TODO !!!! rewrite the whole concept

/**
 * A simple implementation of a linear probing hash table, devoid of fail-fast bells and whistles and similar features.
 * Contrary to Java's standard HashMap, this implementation can reduce its bucket table size when required,
 * preserving the true O(N) scan complexity.
 *
 * @param <E> Entry type
 */
public class LinearHashTable<E> implements ReIterable<E> {
    static final ToIntFunction<Object> DEFAULT_HASH = Object::hashCode;
    static final BiPredicate<Object, Object> DEFAULT_EQUALS = Object::equals;
    private static final IntObjectPredicate<Object> NULL_BIN_PREDICATE = (i, o) -> o == null;

    private static final float loadFactor = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 1 << 1;
    private static final int NULL_VALUE = -1;
    private final int minDataSize;
    private final BiPredicate<E, E> matchFunction;
    int size = 0;
    private Entry[] table;
    private int deletes = 0;
    private int currentInsertIndex;
    private int[] unsignedIndices;
    private int resizeUpperBound;
    private int resizeLowerBound;


    protected LinearHashTable(int minCapacity, BiPredicate<E, E> matchFunction) {
        int capacity = tableSizeFor(minCapacity);
        this.unsignedIndices = new int[capacity];
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;
        this.matchFunction = matchFunction;
        this.minDataSize = capacity;
        this.table = new Entry[capacity];
        this.computeResizeBounds();
    }

    protected LinearHashTable(int minCapacity) {
        this(minCapacity, Objects::equals);
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    private static int tableSizeFor(int dataSize) {
        return CollectionUtils.tableSizeFor(dataSize, loadFactor, MINIMUM_CAPACITY, MAXIMUM_CAPACITY);
    }

    static long location(int hash, int binIndex) {
        return ((long) hash << 32) | (binIndex & 0xFFFFFFFFL);
    }

    static int hash(long location) {
        return (int) (location >> 32);
    }

    static int binIndex(long location) {
        return (int) location;
    }

    // Method to extract two int values from a long
    public static int[] extractInts(long value) {
        int value1 = (int) (value >> 32);
        int value2 = (int) value;

        return new int[]{value1, value2};
    }

    /**
     * <p>
     * Returns the encoded location of a given argument in the hash table. The location is simply
     * the concatenated hash and bin index of where the argument would be placed based on its hash and equality test.
     * This function returns a long to minimize the overhead of Object creation and garbage collection.
     * </p>
     *
     * @param arg           The argument to compute the location from.
     * @param matchFunction The function to test objects with equal hash.
     * @param <K>           The type of the argument.
     * @return The location of the argument in the hash table.
     */
    public <K> long locationFor(@NonNull K arg, BiPredicate<E, K> matchFunction) {
        int hash = arg.hashCode();

        int mask = table.length - 1, counter = 0, pos = hash & mask;
        Entry found;
        while ((found = table[pos]) != null && !found.deleted) {
            if (matchFunction.test(found.value(), arg)) {
                return location(hash, pos);
            } else {
                if (counter++ == table.length) {
                    throw new IllegalStateException("Low-level implementation error, please submit a bug.");
                } else {
                    pos = (pos + 1) & mask;
                }
            }
        }
        return location(hash, pos);
    }

    private <K> Entry entryFor(@NonNull K arg, BiPredicate<E, K> matchFunction) {
        int hash = arg.hashCode();

        int mask = table.length - 1, counter = 0, pos = hash & mask;
        Entry found;
        while ((found = table[pos]) != null && !found.deleted) {
            if (matchFunction.test(found.value(), arg)) {
                return found;
            } else {
                if (counter++ == table.length) {
                    throw new IllegalStateException("Low-level implementation error, please submit a bug.");
                } else {
                    pos = (pos + 1) & mask;
                }
            }
        }
        return new Entry(null, hash, pos);
    }

    void insert(E object) {
        throw new UnsupportedOperationException();
/*
        resize();
        long location = locationFor(object, matchFunction);
        int binIndex = binIndex(location);
        Entry entry = table[binIndex];
        if(entry == null) {
            table[binIndex] = new Entry(object, location);
        } else {
            entry.value = object;
            entry.deleted = false;
        }
*/
    }


/*
    private E saveDirect(E element, int pos) {

        Object old = data[pos];
        data[pos] = element;
        if (old == null) {
            unsignedIndices[currentInsertIndex++] = pos;
            size++;
        } else {
            if (deletedIndices[pos]) {
                deletedIndices[pos] = false;
                deletes--;
                size++;
            }
        }
        return (E) old;
    }
*/

/*
    protected abstract ToIntFunction<Object> getHashFunction();

    protected abstract BiPredicate<Object, Object> getEqualsPredicate();
*/

    public final int size() {
        return size;
    }

/*
    final void deleteEntries(Predicate<E> predicate) {
        int initialDeletes = this.deletes;
        forEachDataEntry((e, i) -> {
            if (predicate.test(e)) {
                markDeleted(i);
            }
        });
        if (initialDeletes != this.deletes) {
            resize();
        }
    }
*/


    protected void markDeleted(int pos) {
        Entry entry = table[pos];
        if (entry != null && !entry.deleted) {
            entry.deleted = true;
            deletes++;
            size--;
        }
    }

/*
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        forEachDataEntry(k -> joiner.add(k.toString()));
        return joiner.toString();
    }
*/

    public void clear() {
        CollectionUtils.systemFill(this.table, null);
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;
        this.size = 0;
        this.deletes = 0;
    }

/*
    // TODO !!!! analyze/rewrite
    boolean containsEntry(E e) {
        int pos = findBinIndexForZ1(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return data[pos] != null && !deletedIndices[pos];
    }

    boolean removeEntry(Object e) {
        int pos = findBinIndexForZ1(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return removeEntry(pos);
    }
*/


    @Override
    public ReIterator<E> iterator() {
        return new It();
    }

/*
    //TODO !!!! analyze usage / rewrite
    public Stream<E> stream() {
        return Arrays.stream(unsignedIndices, 0, currentInsertIndex)
                .filter(i -> !deletedIndices[i])
                .mapToObj(value -> (E) data[value]);
    }
*/

    public void resize() {
        if (size > resizeUpperBound) {
            // Resize up
            rebuild(table.length * 2);
            return;
        } else if (size < resizeLowerBound) {
            // Resize down
            int newDataSize = Math.max(minDataSize, tableSizeFor(resizeLowerBound * 2));
            if (newDataSize != table.length) {
                rebuild(newDataSize);
                return;
            }
        } else {
            if (deletes > size && size > MINIMUM_CAPACITY) {
                // Purge deleted data and indices
                rebuild(this.table.length);
            }
        }

    }

    private void rebuild(int newArrSize) {
/*
        Entry[] newData = new Entry[newArrSize];
        int[] newUnsignedIndices = new int[newArrSize];
        int newCurrentInsertIndex = 0;
        ToIntFunction<Object> hashFunction = Object::hashCode;
        E obj;
        for (int i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(unsignedIndices[i])) != null) {
                int pos = findBinIndex0(hashFunction.applyAsInt(obj), newData, NULL_BIN_PREDICATE);
                newData[pos] = obj;
                newUnsignedIndices[newCurrentInsertIndex++] = pos;
            }
        }
        this.data = newData;
        this.deletes = 0;
        this.deletedIndices = new boolean[newArrSize];
        this.currentInsertIndex = newCurrentInsertIndex;
        this.unsignedIndices = newUnsignedIndices;
*/
        computeResizeBounds();
        throw new UnsupportedOperationException();
    }

    private void computeResizeBounds() {
        this.resizeUpperBound = (int) (table.length * loadFactor);
        this.resizeLowerBound = (int) (table.length * loadFactor / 4);
    }

    private static class Entry {
        private final int hash;
        private final int binIndex;
        private Object value;
        private boolean deleted = false;

        public Entry(@Nullable Object value, int hash, int binIndex) {
            this.value = value;
            this.hash = hash;
            this.binIndex = binIndex;
        }

        @SuppressWarnings("unchecked")
        <T> T value() {
            return (T) value;
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
            throw new UnsupportedOperationException();
/*
            while (pos < currentInsertIndex) {
                int idx = unsignedIndices[pos];
                if (deletedIndices[idx]) {
                    pos++;
                } else {
                    return idx;
                }
            }
            return -1;
*/
        }


        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (nextIndex < 0) {
                throw new NoSuchElementException();
            } else {
                throw new UnsupportedOperationException();
/*
                pos++;
                currentIndex = nextIndex;
                nextIndex = computeNextIndex();
                return (E) data[currentIndex];
*/
            }
        }

        @Override
        public void remove() {
            if (currentIndex < 0) {
                throw new NoSuchElementException();
            } else {
                markDeleted(currentIndex);
            }
        }
    }
}
