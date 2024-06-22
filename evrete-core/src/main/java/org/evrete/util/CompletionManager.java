package org.evrete.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages {@link CompletableFuture} instances associated with given keys.
 * This class provides a mechanism to retrieve or compute a completion for a given key,
 * ensuring that each key is only associated with one future which is removed upon completion.
 *
 * @param <K> the type of keys maintained by this manager
 * @param <T> the type of values associated with the CompletableFutures
 */
public class CompletionManager<K, T> {

    private final ConcurrentHashMap<K, CompletableFuture<T>> completions = new ConcurrentHashMap<>();

    /**
     * Enqueues a new CompletableFuture for the given key. If a future is already associated with the key,
     * the new future is chained to execute after the existing one completes. Regardless of whether the future
     * was newly created or enqueued after an existing one, it is removed from the manager once completed.
     *
     * @param key             the key whose future is to be enqueued or retrieved
     * @param mappingFunction the function to compute a future if none is associated with the key
     * @return a new or updated CompletableFuture associated with the given key
     */
    public CompletableFuture<T> enqueue(K key, Function<? super K, ? extends CompletableFuture<T>> mappingFunction) {
        synchronized (this.completions) {
            CompletableFuture<T> existing = this.completions.get(key);
            final CompletableFuture<T> newFuture;
            if (existing == null) {
                newFuture = mappingFunction.apply(key);
            } else {
                newFuture = existing.thenCompose(t -> mappingFunction.apply(key));
            }

            final CompletableFuture<T> chained = newFuture.whenComplete((t, throwable) -> completions.remove(key));
            this.completions.put(key, chained);
            return chained;

        }
    }

    public int taskCount() {
        return completions.size();
    }

}
