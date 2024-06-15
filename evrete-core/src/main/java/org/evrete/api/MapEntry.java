package org.evrete.api;

import java.util.Map;

public class MapEntry<K, V> {
    private final K key;
    private final V value;

    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public MapEntry(Map.Entry<K,V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
