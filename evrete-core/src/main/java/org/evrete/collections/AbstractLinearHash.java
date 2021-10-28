package org.evrete.collections;

import org.evrete.api.IntObjectPredicate;
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
    static final ToIntFunction<Object> DEFAULT_HASH = Object::hashCode;
    static final BiPredicate<Object, Object> DEFAULT_EQUALS = Object::equals;
    static final IntObjectPredicate<Object> NULL_BIN_PREDICATE = (i, o) -> o == null;

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

    @SuppressWarnings("unchecked")
    private static <Z> int findBinIndex1(int hash, Object[] scope, IntObjectPredicate<Z> predicate) {
        int mask = scope.length - 1, counter = 0, addr = hash & mask;
        while (!predicate.test(addr, (Z) scope[addr])) {
            if (counter++ == scope.length) {
                throw new IllegalStateException("Low-level implementation error, please submit a bug.");
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
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

    public void apply(int hash, Predicate<E> predicate, ObjIntConsumer<E> consumer) {
        apply(hash, binPredicate(predicate), consumer);
    }

    public void apply(int hash, IntObjectPredicate<E> predicate, ObjIntConsumer<E> consumer) {
        int addr = findBinIndex1(hash, this.data, predicate);
        consumer.accept(get(addr), addr);
    }

    public IntObjectPredicate<E> binPredicate(Predicate<E> predicate) {
        return (i, o) -> o == null || deletedIndices[i] || predicate.test(o);
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

    protected void markDeleted(int addr) {
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
        int[] newUnsignedIndices = new int[newArrSize];
        int newCurrentInsertIndex = 0;
        ToIntFunction<Object> hashFunction = getHashFunction();
        E obj;
        for (int i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(unsignedIndices[i])) != null) {
                int addr = findBinIndex1(hashFunction.applyAsInt(obj), newData, NULL_BIN_PREDICATE);
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
