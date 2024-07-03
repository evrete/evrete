package org.evrete.util;

import org.evrete.api.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
