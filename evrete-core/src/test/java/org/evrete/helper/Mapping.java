package org.evrete.helper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Mapping<K, V> {

    V put(K key, V value);

    void forEachEntry(BiConsumer<K, V> consumer);

    void forEachValue(Consumer<V> consumer);

    void forEachKey(Consumer<K> consumer);

    V computeIfAbsent(K key, Function<K, V> function);

    long size();

    V remove(K key);

    V get(K key);

    void clear();

}
