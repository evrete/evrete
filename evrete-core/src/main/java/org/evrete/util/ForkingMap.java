package org.evrete.util;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A basic map data structure that can be used in implementations of the {@link org.evrete.api.Copyable} interface.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ForkingMap<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final ForkingMap<K, V> parent;

    public ForkingMap() {
        this(null);
    }

    private ForkingMap(ForkingMap<K, V> parent) {
        this.parent = parent;
    }

    public ForkingMap<K, V> nextBranch() {
        return new ForkingMap<>(this);
    }

    public Stream<Map.Entry<K, V>> allEntries() {
        Stream<Map.Entry<K, V>> localStream = map.entrySet().stream();
        if (parent == null) {
            return localStream;
        } else {
            return Stream.concat(localStream, parent.allEntries());
        }
    }

    /**
     * Iterates over each key-value pair in the local map and performs the given action.
     *
     * @param action the action to be performed for each key-value pair
     */
    public void forEachLocally(@NonNull BiConsumer<? super K, ? super V> action) {
        this.map.forEach(action);
    }

    /**
     * Iterates over each key-value pair in the tree.
     *
     * @param action the action to be performed for each key-value pair
     */
    public void forEachAll(@NonNull BiConsumer<? super K, ? super V> action) {
        this.map.forEach(action);
        ForkingMap<K, V> p;
        if ((p = this.parent) != null) {
            p.forEachLocally(action);
        }
    }

    /**
     * Retrieves the value associated with the given key from the local map.
     * If the key is not found in the local map, it looks for the value in the parent map(s).
     * If an existing value is found in the parent, it applies a transformation to this value,
     * stores the transformed value in the local map, and then returns it.
     *
     * @param key         The key whose associated value is to be returned or computed.
     * @param transformer A function to compute the new value.
     * @return The current (existing or computed) value associated with the specified key, or {@code null}
     * if no value is found.
     */
    @Nullable
    public V getCreateLocalCopy(K key, UnaryOperator<V> transformer) {
        V locallyFound = map.get(key);
        if (locallyFound != null) {
            return locallyFound;
        } else {
            synchronized (this) {
                return fillFromParent(key, transformer);
            }
        }
    }
    /**
     * Recursively fills a value in the map from the parent structure, applying a transformation.
     *
     * @param key The key whose associated value is to be retrieved and transformed.
     * @param transformer A {@code UnaryOperator} that transforms the retrieved value.
     * @return The transformed value associated with the specified key, or {@code null} if the key
     *         could not be found in the parent structure or the parent structure does not exist.
     */
    V fillFromParent(K key, UnaryOperator<V> transformer) {
        V result;
        if (parent == null || (result = parent.fillFromParent(key, transformer)) == null) {
            return null;
        } else {
            map.put(key, transformer.apply(result));
            return result;
        }
    }

    public V searchExcludeLocal(K key) {
        return parent == null ? null : parent.get(key);
    }

    public V get(K key) {
        V found = map.get(key);
        if (found == null) {
            return parent == null ? null : parent.get(key);
        } else {
            return found;
        }
    }

    public void replace(@NonNull K key, @NonNull V value) {
        K k = Objects.requireNonNull(key);
        V v = Objects.requireNonNull(value);

        V found = map.get(k);
        if (found == null) {
            if (parent != null) {
                parent.replace(k, v);
            }
        } else {
            map.put(k, v);
        }
    }

    public void put(K key, V value) {
        synchronized (this.map) {
            this.map.put(key, value);
        }
    }
}
