package org.evrete.api.spi;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

/**
 * A collection that stores values and assigns them a unique <code>long</code> identifier.
 * The engine uses this API to store fact field values expressed as <code>Object[]</code>.
 * <p>
 * Aside from hashing and equality, the default implementation of this interface essentially works as shown below:
 * </p>
 *
 * <pre><code>
 * private final AtomicLong indexer = new AtomicLong(0);
 * private final Map<Object[], Long> mapping = new ConcurrentHashMap<>();
 * private final Map<Long, Object[]> reverse = new ConcurrentHashMap<>();
 *
 * public long getOrCreateId(Object[] fieldValues) {
 *     return mapping.computeIfAbsent(fieldValues, k -> {
 *         long id = indexer.getAndIncrement();
 *         reverse.put(id, k);
 *         return id;
 *     });
 * }
 * </code></pre>
 * <p>
 * When a {@link org.evrete.api.RuleSession} is created, the engine will generate
 * as many instances of this interface as there are logical types in the ruleset. The implementations
 * must be thread-safe.
 * </p>
 *
 * @param <V> The type of values stored in the collection.
 */
public interface ValueIndexer<V> {
    /**
     * Returns the unique identifier for the specified value, creating a new one if it does not already exist.
     *
     * @param value The value for which the identifier is to be retrieved or created.
     * @return The unique <code>long</code> identifier assigned to the specified value.
     */
    long getOrCreateId(@NonNull V value);

    /**
     * Retrieves the value associated with the given unique identifier.
     *
     * @param id The unique identifier for the desired value.
     * @return The value associated with the specified identifier, or <code>null</code> if no such value exists.
     */
    @Nullable
    V get(long id);

    /**
     * Deletes the value associated with the given unique identifier.
     *
     * @param id The unique identifier for the value to be deleted.
     * @return The value that was associated with the specified identifier, or <code>null</code> if no such value exists.
     */
    @Nullable
    V delete(long id);

    /**
     * Assigns the given value to the specified identifier.
     *
     * @param id The unique identifier for the value.
     * @param value The value to be associated with the specified identifier.
     */
    void assignId(long id, @NonNull V value);

    /**
     * Clears the internally stored mapping.
     */
    void clear();
}
