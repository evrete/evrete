package org.evrete.collections;

import org.evrete.api.BufferedInsert;
import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A simple implementation of linear probing hash table without fail-fast bells and whistles and alike.
 * Unlike the stock Java's HashMap, this implementation can shrink down its bucket table when necessary
 * thus preserving the _real_ O(N) scan complexity. Compared to the Java's HashMap, this implementation
 * is 2-5 times faster.
 *
 * @param <E> Entry type
 */
public abstract class AbstractLinearHash<E> extends UnsignedIntArray implements ReIterable<E>, BufferedInsert {
    protected static final BiPredicate<Object, Object> IDENTITY_EQUALS = (o1, o2) -> o1 == o2;
    static final ToIntFunction<Object> DEFAULT_HASH = Object::hashCode;
    static final ToIntFunction<Object> IDENTITY_HASH = System::identityHashCode;
    static final BiPredicate<Object, Object> DEFAULT_EQUALS = Object::equals;
    private static final int DEFAULT_INITIAL_CAPACITY = 4;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 2;
    Object[] data;
    boolean[] deletedIndices;
    int size = 0;
    int deletes = 0;

    AbstractLinearHash(int initialCapacity) {
        super(initialCapacity);
        int capacity = tableSizeFor(initialCapacity);
        this.data = new Object[capacity];
        this.deletedIndices = new boolean[capacity];
    }

    protected AbstractLinearHash() {
        this(DEFAULT_INITIAL_CAPACITY);
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
        E found;
        while ((found = (E) destination[addr]) != null) {
            if (eqTest.test(found, key)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
    }

    private static int findBinIndexFor(int hash, Object[] destination, Predicate<Object> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        Object found;
        while ((found = destination[addr]) != null) {
            if (eqTest.test(found)) {
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
    private static int tableSizeFor(int capacity) {
        int cap = Math.max(capacity, MINIMUM_CAPACITY);
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
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

    int findBinIndexFor(int hash, Predicate<Object> eqTest) {
        return findBinIndexFor(hash, data, eqTest);
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

    private void addNoResize(E element) {
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, getEqualsPredicate());
        saveDirect(element, addr);
    }

    public final <Z extends AbstractLinearHash<E>> void bulkAdd(Z other) {
        resize(size + other.size);

        ToIntFunction<Object> hashFunc = getHashFunction();
        BiPredicate<Object, Object> eqPredicate = getEqualsPredicate();

        int i, idx;
        E o;
        for (i = 0; i < other.currentInsertIndex; i++) {
            idx = other.getAt(i);
            if ((o = other.get(idx)) != null) {
                int hash = hashFunc.applyAsInt(o);
                int addr = findBinIndexFor(o, hash, eqPredicate);
                saveDirect(o, addr);
            }
        }
    }


    @Override
    public final void ensureExtraCapacity(int insertCount) {
        resize(size + insertCount);
    }

    @SuppressWarnings("unchecked")
    public final E saveDirect(E element, int addr) {
        Object old = data[addr];
        data[addr] = element;
        if (old == null) {
            addNew(addr);
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

    final int dataSize() {
        return data.length;
    }

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
        int i;
        E obj;
        for (i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(getAt(i))) != null) {
                consumer.accept(obj);
            }
        }
    }

    private void forEachDataEntry(ObjIntConsumer<E> consumer) {
        int i, idx;
        E obj;
        for (i = 0; i < currentInsertIndex; i++) {
            idx = getAt(i);
            if ((obj = get(idx)) != null) {
                consumer.accept(obj, idx);
            }
        }
    }

    protected void markDeleted(int addr) {
        deletedIndices[addr] = true;
        deletes++;
        size--;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        forEachDataEntry(k -> joiner.add(k.toString()));
        return joiner.toString();
    }

    public void clear() {
        super.clear();
        CollectionUtils.systemFill(this.data, null);
        CollectionUtils.systemFill(this.deletedIndices, false);
        this.size = 0;
        this.deletes = 0;
    }

    boolean containsEntry(E e) {
        int addr = findBinIndexFor(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return data[addr] != null && !deletedIndices[addr];
    }

    protected boolean removeEntry(Object e) {
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
                removeNonEmpty(addr);
                return true;
            }
        }
    }

    private void removeNonEmpty(int addr) {
        markDeleted(addr);
        resize();
    }

    @Override
    public ReIterator<E> iterator() {
        return new It();
    }

    @SuppressWarnings("unchecked")
    public Stream<E> stream() {
        return intStream().filter(i -> !deletedIndices[i]).mapToObj(value -> (E) data[value]);
    }


    protected void resize() {
        assert currentInsertIndex() == this.size + this.deletes : "indices: " + currentInsertIndex() + " size: " + this.size + ", deletes: " + this.deletes;
        resize(this.size);
    }

    public void resize(int targetSize) {
        boolean expand = 2 * (targetSize + deletes) >= data.length;
        boolean shrink = deletes > 0 && targetSize < deletes;
        if (expand || shrink) {
            int newSize = tableSizeFor(Math.max(MINIMUM_CAPACITY, targetSize * 2 + 1));
            if (newSize > MAXIMUM_CAPACITY) throw new OutOfMemoryError();

            Object[] newData = (Object[]) Array.newInstance(data.getClass().getComponentType(), newSize);
            UnsignedIntArray newIndices = new UnsignedIntArray(newSize);

            if (targetSize > 0) {
                ToIntFunction<Object> hashFunction = getHashFunction();
                forEachDataEntry(e -> {
                    int addr = findEmptyBinIndex(hashFunction.applyAsInt(e), newData);
                    newData[addr] = e;
                    newIndices.addNew(addr);
                });
            }

            this.data = newData;
            this.copyFrom(newIndices);
            this.deletes = 0;
            this.deletedIndices = new boolean[newSize];
        }

    }

    void assertStructure() {
        int indices = currentInsertIndex();
        int deletes = this.deletes;
        assert indices == size + deletes : "indices: " + indices + " size: " + size + ", deletes: " + deletes;
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
                int idx = getAt(pos);
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
