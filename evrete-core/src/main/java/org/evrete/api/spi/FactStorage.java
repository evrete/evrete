package org.evrete.api.spi;

import org.evrete.api.FactHandle;
import org.evrete.api.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;


/**
 * A fact storage interface
 *
 * @param <FH> the type of the fact handle
 * @param <V>  the type of the value associated with the fact handle
 */
public interface FactStorage<FH extends FactHandle, V> {

    /**
     * Inserts a fact into the delta storage.
     *
     * @param factHandle the handle for the fact to be inserted
     * @param value      the value associated with the fact handle
     */
    void insert(FH factHandle, V value);

    /**
     * Deletes a fact from the memory for both {@link MemoryScope#DELTA} and
     * {@link MemoryScope#MAIN} scopes.
     *
     * @param factHandle the previously assigned fact handle
     * @return the previously existing value, or {@code null} if no value was associated with the fact handle
     */
    V remove(FH factHandle);

    /**
     * Retrieves a fact value by fact handle.
     *
     * @param factHandle the handle for the fact to be retrieved
     * @return the value associated with the fact handle, or null if not found
     */
    @Nullable V get(FH factHandle);


    /**
     * Returns stream of currently stored values.
     * @return stream of currently stored mappings.
     */
    Stream<Map.Entry<FH, V>> stream();

    /**
     * Clears the memory
     */
    void clear();

}


