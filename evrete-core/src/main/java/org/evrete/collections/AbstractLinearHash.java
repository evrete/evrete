package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A simple implementation of a linear probing hash table, devoid of fail-fast bells and whistles and similar features.
 * Contrary to Java's standard HashMap, this implementation can reduce its bucket table size when required,
 * thus preserving the true O(N) scan complexity.
 *
 * @param <E> Entry type
 */
public abstract class AbstractLinearHash<E> implements ReIterable<E> {

    private static final float loadFactor = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 1 << 1;
    private static final int NULL_VALUE = -1;
    private final int minDataSize;
    int size = 0;
    private Object[] data;
    private boolean[] deletedIndices;
    private int deletes = 0;
    private int currentInsertIndex;
    private int[] unsignedIndices;

    protected AbstractLinearHash(int minCapacity) {
        int capacity = tableSizeFor(minCapacity);
        this.unsignedIndices = new int[capacity];
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;

        this.minDataSize = capacity;
        this.data = new Object[capacity];
        this.deletedIndices = new boolean[capacity];
    }

    private static int findEmptyBin(int hash, int mask, Object[] destination) {
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

    @SuppressWarnings("unchecked")
    private <T> int findBinIndexRaw(T key, int hash, BiPredicate<E, T> eqTest) {
        final int size = data.length, mask = size - 1;
        int pos = hash & mask, counter = 0;
        Object found;
        while ((found = data[pos]) != null) {
            if (eqTest.test((E) found, key)) {
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
     * Returns a power of two size for the given target capacity.
     */
    private static int tableSizeFor(int dataSize) {
        int capacity = (int) (dataSize / loadFactor);
        int cap = Math.max(capacity, MINIMUM_CAPACITY);
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        int ret = n + 1;
        assert ret >= capacity;
        if (ret > MAXIMUM_CAPACITY) throw new OutOfMemoryError();
        return ret;
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
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        E existing = this.get(binIndex);
        if (existing == null) {
            this.saveDirect(producer.apply(key), binIndex);
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
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        E existing = this.get(binIndex);
        if (existing == null) {
            E newValue = producer.apply(hash, key);
            this.saveDirect(newValue, binIndex);
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
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        E result = this.get(binIndex);
        if (result == null) {
            result = producer.apply(key);
            this.saveDirect(result, binIndex);
        }
        return result;
    }

    public <K> boolean remove(K key, BiPredicate<E, K> searchFunction) {
        this.resize();
        int hash = key.hashCode();
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        return deleteEntry(binIndex);
    }

    public <K> boolean contains(K key, BiPredicate<E, K> searchFunction) {
        int hash = key.hashCode();
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        return this.get(binIndex) != null;
    }

    public <K> E get(K key, BiPredicate<E, K> searchFunction) {
        int hash = key.hashCode();
        int binIndex = this.findBinIndexRaw(key, hash, searchFunction);
        return this.get(binIndex);
    }

    @SuppressWarnings("unchecked")
    private E get(int pos) {
        return deletedIndices[pos] ? null : (E) data[pos];
    }

    /**
     * Adds an element to the collection.
     *
     * @param element the element to be added (must not be null)
     * @return true if this collection did not already contain the specified element
     */
    public final boolean add(@NonNull E element) {
        return replace(element) == null;
    }

    /**
     * Replaces the element in the collection with the specified element.
     *
     * @param element the element to be replaced
     * @return the replaced element, or null if the element was not found in the collection
     */
    public final E replace(E element) {
        resize();
        int pos = findBinIndexRaw(element, element.hashCode(), getEqualsPredicate());
        return saveDirect(element, pos);
    }


    @SuppressWarnings("unchecked")
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


    protected abstract BiPredicate<E, E> getEqualsPredicate();

    public final int size() {
        return size;
    }

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
    public void addAll(AbstractLinearHash<E> source, BinaryOperator<E> combineFunction) {
        BiPredicate<E, E> matchFunction = getEqualsPredicate();
        source.forEachDataEntry(externalElement -> {
            resize();
            int hash = externalElement.hashCode();
            int binIndex = findBinIndexRaw(externalElement, hash, matchFunction);
            @Nullable E result = combineFunction.apply(get(binIndex), externalElement);

            if (result == null) {
                deleteEntry(binIndex);
            } else {
                saveDirect(result, binIndex);
            }
        });
    }

    public void forEachDataEntry(Consumer<E> consumer) {
        E obj;
        for (int i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(unsignedIndices[i])) != null) {
                consumer.accept(obj);
            }
        }
    }


    private void forEachDataEntry(ObjIntConsumer<E> consumer) {
        int i, idx;
        E obj;
        for (i = 0; i < currentInsertIndex; i++) {
            idx = unsignedIndices[i];
            if ((obj = get(idx)) != null) {
                consumer.accept(obj, idx);
            }
        }
    }

    protected boolean markDeleted(int pos) {
        if (deletedIndices[pos]) {
            // Already deleted
            return false;
        } else {
            deletedIndices[pos] = true;
            deletes++;
            size--;
            return true;
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
        CollectionUtils.systemFill(this.deletedIndices, false);
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;
        this.size = 0;
        this.deletes = 0;
    }

    public boolean contains(E e) {
        return contains(e, getEqualsPredicate());
    }

    public boolean remove(E e) {
        return remove(e, getEqualsPredicate());
    }

    private boolean deleteEntry(int pos) {
        if (data[pos] == null) {
            // Nothing to delete
            return false;
        } else {
            return markDeleted(pos);
        }
    }

    @Override
    @NonNull
    public ReIterator<E> iterator() {
        return new It();
    }

    @SuppressWarnings("unchecked")
    public Stream<E> stream() {
        return Arrays.stream(unsignedIndices, 0, currentInsertIndex)
                .filter(i -> !deletedIndices[i])
                .mapToObj(value -> (E) data[value]);
    }

    public void resize() {
        int upperBound = (int) (data.length * loadFactor);
        int lowerBound = (int) (data.length * loadFactor / 4);
        if (size > upperBound) {
            // Resize up
            rebuild(data.length * 2);
            return;
        }

        if (size < lowerBound) {
            // Resize down
            int newDataSize = Math.max(minDataSize, tableSizeFor(lowerBound * 2));
            if (newDataSize != data.length) {
                rebuild(newDataSize);
                return;
            }
        }

        if (deletes > size && size > MINIMUM_CAPACITY) {
            // Purge deleted data and indices
            rebuild(this.data.length);
        }
    }

    private void rebuild(int newArrSize) {
        Object[] newData = new Object[newArrSize];
        int mask = newArrSize - 1;
        int[] newUnsignedIndices = new int[newArrSize];
        int newCurrentInsertIndex = 0;
        E obj;
        for (int i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(unsignedIndices[i])) != null) {
                int pos = findEmptyBin(obj.hashCode(), mask, newData);
                newData[pos] = obj;
                newUnsignedIndices[newCurrentInsertIndex++] = pos;
            }
        }
        this.data = newData;
        this.deletes = 0;
        this.deletedIndices = new boolean[newArrSize];
        this.currentInsertIndex = newCurrentInsertIndex;
        this.unsignedIndices = newUnsignedIndices;
    }

    void assertStructure() {
        int indices = currentInsertIndex;
        int deletes = this.deletes;
        assert indices == size + deletes : "indices: " + indices + " size: " + size + ", deletes: " + deletes;
        assert this.data.length >= minDataSize;
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
            while (pos < currentInsertIndex) {
                int idx = unsignedIndices[pos];
                if (deletedIndices[idx]) {
                    pos++;
                } else {
                    return idx;
                }
            }
            return -1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (nextIndex < 0) {
                throw new NoSuchElementException();
            } else {
                pos++;
                currentIndex = nextIndex;
                nextIndex = computeNextIndex();
                return (E) data[currentIndex];
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
