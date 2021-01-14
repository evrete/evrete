package org.evrete.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MapFunction<K, V> implements Function<K, V> {
    private final Map<K, V> map;

    public MapFunction() {
        this.map = new HashMap<>();
    }

    public void putNew(K key, V value) {
        if (map.put(key, value) != null) {
            throw new IllegalStateException("Key " + key + " is already associated with a value.");
        }
    }

    @Override
    public V apply(K k) {
        V found = map.get(k);
        if (found == null) {
            throw new IllegalArgumentException("No data can be found for key " + k);
        } else {
            return found;
        }
    }
}
