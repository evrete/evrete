package org.evrete.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A class that implements a {@link Function} interface using an internal map.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class MapFunction<K, V> implements Function<K, V> {
    private final Map<K, V> map;

    public MapFunction() {
        this.map = new HashMap<>();
    }

    public MapFunction(Collection<V> collection, Function<V, K> keyMapper) {
        this();
        for (V v : collection) {
            this.putNew(keyMapper.apply(v), v);
        }
    }

    public void putNew(K key, V value) {
        if (map.put(key, value) != null) {
            throw new IllegalStateException("Key " + key + " is already associated with a value.");
        }
    }

    public int size() {
        return map.size();
    }

    public Collection<V> values() {
        return map.values();
    }

    @Override
    public V apply(K k) {
        V found = map.get(k);
        if (found == null) {
            throw new IllegalArgumentException("No object can be found for key " + k);
        } else {
            return found;
        }
    }
}
