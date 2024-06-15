package org.evrete.collections;

import org.evrete.util.Indexed;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A replacement for an ordinary array that can spawn new branches.
 * <p>
 * Once spawned, a new branch keeps its own indexing and becomes independent of other branches spawned
 * from the same parent. When a value is requested by index, this class returns its own value or delegates
 * the request to its parent node. Internally, this class stores values along with their array indices in
 * the form of {@link Indexed} instances. Other than that, the {@link #get(int)} method behaves exactly as <code>T[index]</code>.
 * </p>
 * <p>
 * The implementation supports additions and read operations only.
 * Later additions made to the instance's parent branch are not visible to that instance's read operations.
 * </p>
 *
 * @param <V> the internal storage type.
 */
//TODO review the whole package, or even delete it as it's not exported
public class ForkingArray<V extends Indexed> {
    static final int DEFAULT_INITIAL_SIZE = 16;
    private Object[] array;
    private final ForkingArray<V> parent;
    private final int dataOffset;
    private int nextWriteIndex;
    private final int initialArraySize;
    private final int lastKnownParentIndex;

    private ForkingArray(int initialArraySize, ForkingArray<V> parent) {
        if (initialArraySize < 1) {
            throw new IllegalArgumentException("Initial array size must be greater than 0");
        } else {
            this.array = new Object[initialArraySize];
            this.initialArraySize = initialArraySize;
            this.nextWriteIndex = 0;
            this.parent = parent;
            if (parent == null) {
                this.dataOffset = 0;
                this.lastKnownParentIndex = 0;
            } else {
                this.dataOffset = parent.dataOffset + parent.nextWriteIndex;
                this.lastKnownParentIndex = parent.nextWriteIndex;
            }
        }
    }

    public ForkingArray() {
        this(DEFAULT_INITIAL_SIZE);
    }

    public ForkingArray(int initialArraySize) {
        this(initialArraySize, null);
    }

    int getDataOffset() {
        return dataOffset;
    }

    int getInitialArraySize() {
        return initialArraySize;
    }

    public void forEach(Consumer<? super V> consumer) {
        int i, size = this.size();
        for (i = 0; i < size; i++) {
            consumer.accept(this.get(i));
        }
    }

    public int size() {
        return this.nextWriteIndex + this.dataOffset;
    }

    //TODO Get rid of this
    public Stream<V> stream(boolean parallel) {
        Stream<V> stream = IntStream.range(0, size()).mapToObj(this::get);
        return parallel ? stream.parallel() : stream;
    }

    public Stream<V> stream() {
        return IntStream.range(0, size()).mapToObj(this::get);
    }

    public ForkingArray<V> newBranch() {
        return new ForkingArray<>(this.initialArraySize, this);
    }

    /**
     * Appends a new value to the array. This method allocates a new index for the new element and
     * passes it to the mapping function to create the value that will be stored at that index.
     *
     * @param element the element to transform into an array value
     * @param mapper the mapping function to create the stored value from the provided argument and the next array index
     * @param <T> the type of the argument
     * @return the new value, which can be accessed via the same index that was passed to the mapping function
     */
    public synchronized <T> V append(T element, ObjIntFunction<T, V> mapper) {
        if (nextWriteIndex >= array.length) {
            this.array = Arrays.copyOf(array, array.length * 2);
        }

        V ret = mapper.apply(nextWriteIndex + this.dataOffset, element);
        this.array[nextWriteIndex] = ret;
        nextWriteIndex++;
        return ret;
    }

    @SuppressWarnings("unchecked")
    public V get(int index) {
        int adjustedIndex = index - this.dataOffset;
        if (adjustedIndex < 0) {
            // This index belongs to a parent level
            if (parent == null) {
                return null;
            } else {
                return parent.get(index);
            }
        } else if (adjustedIndex >= this.nextWriteIndex) {
            return null;
        } else {
            return (V) array[adjustedIndex];
        }
    }
}
