package org.evrete.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MapFunction<K, V> implements Function<K, V> {
    private final Map<K, V> map;

    public MapFunction() {
        this.map = new HashMap<>();
    }

    public static <K1, V1> Function<K1, V1> union(MapFunction<K1, V1> f1, MapFunction<K1, V1> f2) {
        return key -> {
            V1 result = f1.map.getOrDefault(key, f2.map.get(key));
            if (result == null) {
                throw new IllegalStateException();
            } else {
                return result;
            }
        };
    }

    public V put(K key, V value) {
        return map.put(key, value);
    }

    public void putNew(K key, V value) {
        if (map.put(key, value) != null) {
            throw new IllegalStateException();
        }
    }

    public Collection<V> values() {
        return map.values();
    }

    @Override
    public V apply(K k) {
        V found = map.get(k);
        if (found == null) {
            throw new IllegalStateException();
        } else {
            return found;
        }
    }
}
