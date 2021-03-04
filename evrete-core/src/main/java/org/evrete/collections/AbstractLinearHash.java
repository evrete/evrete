package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A simple implementation of linear probing hash table without fail-fast bells and whistles and alike.
 * Unlike the stock Java's HashMap, this implementation can shrink down its bucket table when necessary,
 * thus preserving the real O(N) scan complexity. See benchmark tests for performance comparisons.
 *
 * @param <E> Entry type
 */
public abstract class AbstractLinearHash<E> implements ReIterable<E> {
    private static final float loadFactor = 0.75f;
    static final ToIntFunction<Object> DEFAULT_HASH = Object::hashCode;
    static final BiPredicate<Object, Object> DEFAULT_EQUALS = Object::equals;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 1 << 1;
    int size = 0;
    private Object[] data;
    private boolean[] deletedIndices;
    private int deletes = 0;
    private static final int NULL_VALUE = -1;
    private final int minDataSize;
    int currentInsertIndex;
    int[] unsignedIndices;

    protected AbstractLinearHash(int minCapacity) {
        int capacity = tableSizeFor(minCapacity);
        this.unsignedIndices = new int[capacity];
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;

        this.minDataSize = capacity;
        this.data = new Object[capacity];
        this.deletedIndices = new boolean[capacity];
    }

    private static int findBinIndexFor(Object key, int hash, Object[] destination, BiPredicate<Object, Object> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        Object found;
        while ((found = destination[addr]) != null) {
            if (eqTest.test(key, found)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
    }

    private static int findEmptyBinIndex(int hash, Object[] destination) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        while (destination[addr] != null) {
            addr = (addr + 1) & mask;
        }
        return addr;
    }

    @SuppressWarnings("unchecked")
    private static <K, E> int findBinIndex(K key, int hash, Object[] destination, BiPredicate<E, K> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        Object found;
        while ((found = destination[addr]) != null) {
            if (eqTest.test((E) found, key)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
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

    @SuppressWarnings("unchecked")
    public E get(int addr) {
        return deletedIndices[addr] ? null : (E) data[addr];
    }

    private int findBinIndexFor(Object key, int hash, BiPredicate<Object, Object> eqTest) {
        return findBinIndexFor(key, hash, data, eqTest);
    }

    public <K> int findBinIndex(K key, int hash, BiPredicate<? super E, K> eqTest) {
        return findBinIndex(key, hash, data, eqTest);
    }

    public final boolean addVerbose(E element) {
        resize();
        BiPredicate<Object, Object> eq = getEqualsPredicate();
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, eq);
        E old = saveDirect(element, addr);
        return old == null || !eq.test(element, old);
    }

    public final void addSilent(E element) {
        resize();
        addNoResize(element);
    }

    @SuppressWarnings("unused")
    public final E add(E element) {
        resize();
        return addGetPrevious(element);
    }

    private void addNoResize(E element) {
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, getEqualsPredicate());
        saveDirect(element, addr);
    }

    private E addGetPrevious(E element) {
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, getEqualsPredicate());
        return saveDirect(element, addr);
    }

    protected final <Z extends AbstractLinearHash<E>> void bulkAdd(Z other) {
        ensureExtraCapacity(other.size);

        ToIntFunction<Object> hashFunc = getHashFunction();
        BiPredicate<Object, Object> eqPredicate = getEqualsPredicate();

        int i, idx;
        E o;
        for (i = 0; i < other.currentInsertIndex; i++) {
            idx = other.unsignedIndices[i];
            if ((o = other.get(idx)) != null) {
                int hash = hashFunc.applyAsInt(o);
                int addr = findBinIndexFor(o, hash, eqPredicate);
                saveDirect(o, addr);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public final E saveDirect(E element, int addr) {
        Object old = data[addr];
        data[addr] = element;
        if (old == null) {
            unsignedIndices[currentInsertIndex++] = addr;
            size++;
        } else {
            if (deletedIndices[addr]) {
                deletedIndices[addr] = false;
                deletes--;
                size++;
            }
        }
        return (E) old;
    }

    protected abstract ToIntFunction<Object> getHashFunction();

    protected abstract BiPredicate<Object, Object> getEqualsPredicate();

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

    public void markDeleted(int addr) {
        if (!deletedIndices[addr]) {
            deletedIndices[addr] = true;
            deletes++;
            size--;
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

    boolean containsEntry(E e) {
        int addr = findBinIndexFor(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return data[addr] != null && !deletedIndices[addr];
    }

    boolean removeEntry(Object e) {
        int addr = findBinIndexFor(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return removeEntry(addr);
    }

    private boolean removeEntry(int addr) {
        if (data[addr] == null) {
            // Nothing to delete
            return false;
        } else {
            if (deletedIndices[addr]) {
                // Nothing to delete
                return false;
            } else {
                markDeleted(addr);
                return true;
            }
        }
    }

    @Override
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
            int newDataSize = data.length * 2;
            rebuild(newDataSize);
            return;
        } else if (size < lowerBound) {
            // Resize down
            int newDataSize = Math.max(minDataSize, tableSizeFor(lowerBound * 2));
            if (newDataSize != data.length) {
                rebuild(newDataSize);
                return;
            }
        }
        purgeIfNecessary();
    }


    private void rebuild(int newArrSize) {
        if (newArrSize == this.data.length) {
            throw new IllegalStateException();
        } else {
            Object[] newData = new Object[newArrSize];
            int[] newUnsignedIndices = new int[newArrSize];
            int newCurrentInsertIndex = 0;
            ToIntFunction<Object> hashFunction = getHashFunction();
            E obj;
            for (int i = 0; i < currentInsertIndex; i++) {
                if ((obj = get(unsignedIndices[i])) != null) {
                    int addr = findEmptyBinIndex(hashFunction.applyAsInt(obj), newData);
                    newData[addr] = obj;
                    newUnsignedIndices[newCurrentInsertIndex++] = addr;
                }
            }
            this.data = newData;
            this.deletes = 0;
            this.deletedIndices = new boolean[newArrSize];
            this.currentInsertIndex = newCurrentInsertIndex;
            this.unsignedIndices = newUnsignedIndices;
        }
    }

    private void purgeIfNecessary() {
        if (size > 4 && deletes > size) {
            int[] newUnsignedIndices = new int[this.data.length];
            int newCurrentInsertIndex = 0;
            for (int i = 0; i < currentInsertIndex; i++) {
                int addr = unsignedIndices[i];
                if (deletedIndices[addr]) {
                    this.data[addr] = null;
                } else {
                    newUnsignedIndices[newCurrentInsertIndex++] = addr;
                }
            }
            this.deletes = 0;
            this.deletedIndices = new boolean[this.data.length];
            this.currentInsertIndex = newCurrentInsertIndex;
            this.unsignedIndices = newUnsignedIndices;
        }
    }


    public void ensureExtraCapacity(int expectedNewObjectCount) {
        int expectedNewSize = this.size + expectedNewObjectCount;
        int newArrSize = tableSizeFor((int) (2 * expectedNewSize / loadFactor));
        if (newArrSize != this.data.length) {
            rebuild(newArrSize);
        } else {
            purgeIfNecessary();
        }
    }

    void assertStructure() {
        int indices = currentInsertIndex;
        int deletes = this.deletes;
        assert indices == size + deletes : "indices: " + indices + " size: " + size + ", deletes: " + deletes;
        assert this.data.length >= minDataSize;
    }

    private class It implements ReIterator<E> {
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
