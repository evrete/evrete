package org.evrete.api;

public interface MappingReIterable<K, V> extends ReIterable<MappingReIterable.Entry<K, V>> {

    interface Entry<K, V> extends ReIterable<V> {
        K key();
    }
}
