package org.evrete.api.spi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * General interface for two-stage memory instances where insert operations
 * are buffered in an internal delta storage and then committed to the main storage
 * by invoking the {@link #commit()} method.
 */
public interface DeltaInsertMemory {

    /**
     * Converts the accumulated delta changes into main (permanent) storage.
     */
    void commit();

    /**
     * Clears all elements from both delta and main storage.
     */
    void clear();


    default CompletableFuture<Void> commit(ExecutorService executor) {
        return CompletableFuture.runAsync(this::commit, executor);
    }

}

