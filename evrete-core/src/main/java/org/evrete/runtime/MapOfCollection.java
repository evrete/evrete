package org.evrete.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

public class MapOfCollection<K, V, C extends Collection<V>> extends HashMap<K, C> {
    private final Function<K, C> collectionSupplier;

    public MapOfCollection(Function<K, C> collectionSupplier) {
        this.collectionSupplier = collectionSupplier;
    }

    public  <T> void addAll(Collection<T> collection, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        collection.forEach(element -> add(keyFunction.apply(element), valueFunction.apply(element)));
    }

    public final void add(K k, V v) {
        computeIfAbsent(k, collectionSupplier).add(v);
    }

}
