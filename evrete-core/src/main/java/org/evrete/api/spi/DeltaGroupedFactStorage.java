package org.evrete.api.spi;

import org.evrete.api.ReteMemory;
import org.evrete.api.annotations.NonNull;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>
 * A map-like storage that holds objects grouped by keys, similar to
 * <code>Map&lt;Long,&nbsp;Set&lt;V&gt;&gt;</code>. Internally, the memory is split into two parts:
 * a main memory and a delta memory for buffering insertions. Upon calling the {@link #commit()} method,
 * the implementation empties the delta memory by moving all the data into the main memory.
 * </p>
 *
 * @param <V> the value type
 */
//TODO rename !!!
public interface DeltaGroupedFactStorage<V> extends ReteMemory<Long> {

    /**
     * Buffers a new key/value combination into the delta memory.
     *
     * @param key   the key to be inserted
     * @param value the value associated with the key
     * @throws NullPointerException if the key or value is null
     */
    void insert(long key, @NonNull V value);

    /**
     * Deletes the given key and value from both delta and main memories.
     * If the value is the last one stored under this key, the key must be deleted as well.
     *
     * @param key   the key to be deleted
     * @param value the value associated with the key
     * @throws NullPointerException if the key or value is null
     */
    void delete(long key, @NonNull V value);


    /**
     * Returns an iterator over values.
     *
     * @param scope the scope of the values (not the keys)
     * @param key   the key identifying the values to iterate over
     * @return an iterator over the values associated with the given key in the specified scope
     */
    Iterator<V> valueIterator(MemoryScope scope, long key);


    /**
     * Provides a default implementation for streaming over stored elements based on the provided key and scope.
     * Implementations are encouraged to override this method to create parallel streams, if possible.
     *
     * @param scope The memory scope indicating whether to stream over main or delta storage.
     * @param key   The key identifying the collection of values to stream.
     * @return A stream over elements in the storage of the provided scope.
     */
    default Stream<V> stream(MemoryScope scope, long key) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(valueIterator(scope, key), 0),
                false
        );
    }

    default Iterator<Long> keyIterator(MemoryScope scope) {
        return iterator(scope);
    }
}
