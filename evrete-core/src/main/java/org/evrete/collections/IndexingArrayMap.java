package org.evrete.collections;

import org.evrete.api.MapEntry;
import org.evrete.util.Indexed;

import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A class that provides auto-indexing of elements of the given type based on their computed unique key.
 * The resulting indexes are auto-incremented from <code>0</code> and can be used as arguments for the
 * {@link #get(int)} or {@link #getUnchecked(int)} methods.
 *
 * @param <T> the type of the elements being indexed
 * @param <MatchKey> the type of the computed unique key
 * @param <Stored> the resulting type that is generated from the indexed element and assigned index
 */
//TODO tests!!!!
public abstract class IndexingArrayMap<T, MatchKey, FastKey extends Indexed, Stored> {
    private static final int DEFAULT_INITIAL_SIZE = 16;
    private final HashMap<MatchKey, Integer> keyMap;
    private InnerMapEntry<FastKey, Stored>[] array;
    private final Function<T, MatchKey> keyFunction;
    private int nextWriteIndex;

    /**
     * Generates a unique key for the given value. This key will be used for accessing
     * the value within the map.
     *
     * @param value the value for which the key is to be generated
     * @param index the index at which the value is to be stored
     * @return a generated key for the given value
     */
    protected abstract FastKey generateKey(T value, int index);

    /**
     * Generates the stored value using the provided key and original value. This
     * stored value is what will be stored in this map.
     *
     * @param key   the generated key for the value
     * @param value the original value to be stored
     * @return the generated stored value
     */
    protected abstract Stored generateValue(FastKey key, T value);

    /**
     * Default constructor
     *
     * @param keyFunction    the function that maps the source object to unique keys
     */
    @SuppressWarnings("unchecked")
    public IndexingArrayMap(Function<T, MatchKey> keyFunction) {
        this.keyMap = new HashMap<>();
        this.keyFunction = keyFunction;
        this.array = (InnerMapEntry<FastKey, Stored>[]) new InnerMapEntry<?,?>[DEFAULT_INITIAL_SIZE];
    }

    /**
     * Shallow copy-all constructor
     *
     * @param other parent instance
     */
    protected IndexingArrayMap(IndexingArrayMap<T, MatchKey, FastKey, Stored> other) {
        this.keyMap = new HashMap<>(other.keyMap);
        this.array = other.array.clone();
        this.keyFunction = other.keyFunction;
        this.nextWriteIndex = other.nextWriteIndex;
    }

    /**
     * Deep copy-all constructor
     *
     * @param other          parent instance
     * @param updateFunction function to apply for each stored value
     */
    protected IndexingArrayMap(IndexingArrayMap<T, MatchKey, FastKey, Stored> other, UnaryOperator<Stored> updateFunction) {
        this(other);
        Stored v;
        for (int i = 0; i < nextWriteIndex; i++) {
            v = array[i].getValue();
            array[i] = array[i].clone(updateFunction.apply(v));
        }
    }

    public int size() {
        return this.array.length;
    }

    public Stored get(FastKey key) {
        return array[key.getIndex()].getValue();
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<Stored> consumer) {
        synchronized (keyMap) {
            for (Object o : array) {
                consumer.accept((Stored) o);
            }
        }
    }

    public Stream<Stored> values() {
        return Arrays.stream(this.array).filter(Objects::nonNull).map(MapEntry::getValue);
    }

    /**
     * Retrieves a value associated with the given argument, or creates a new value if it doesn't exist.
     * When creating a new value, a unique int index will be provided to the constructor method. You can use
     * that index later for fast access via the {@link #get(int)} or {@link #getUnchecked(int)} methods.
     *
     * @param arg the argument used to retrieve or create the value
     * @return the value associated with the given argument
     */
    public MapEntry<FastKey, Stored> getOrCreateEntry(T arg) {
        synchronized (keyMap) {
            MatchKey key = keyFunction.apply(arg);
            Integer index = keyMap.computeIfAbsent(key, k -> {
                ensureCapacity();
                int idx = nextWriteIndex++;
                FastKey fastKey = generateKey(arg, idx);
                Stored stored = generateValue(fastKey, arg);
                InnerMapEntry<FastKey, Stored> entry = new InnerMapEntry<>(fastKey, stored);
                this.array[idx] = entry;
                return idx;
            });
            return this.array[index];
        }
    }

    private void ensureCapacity() {
        if(nextWriteIndex >= array.length) {
            this.array = Arrays.copyOf(array, array.length * 2);
        }
    }

    /**
     * Gets the value by its previously assigned unique index.
     * @param index the index
     * @return previously indexed value
     * @throws NoSuchElementException if the index is out of range
     */
    public Stored get(int index) {
        if (index < 0 || index >= array.length) {
            throw new NoSuchElementException("No such index: " + index + " in index map " + this.getClass().getName());
        } else {
            return getUnchecked(index);
        }
    }


    /**
     * Gets the value by its previously assigned unique index.
     * @param index the index
     * @return previously indexed value
     */
    @SuppressWarnings("unchecked")
    public Stored getUnchecked(int index) {
        return (Stored) array[index];
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }

    static class InnerMapEntry<FastKey extends Indexed, Stored> extends MapEntry<FastKey, Stored> {

        InnerMapEntry(FastKey key, Stored value) {
            super(key, value);
        }

        private InnerMapEntry<FastKey, Stored> clone(Stored newValue) {
            return new InnerMapEntry<>(this.getKey(), newValue);
        }


    }
}
