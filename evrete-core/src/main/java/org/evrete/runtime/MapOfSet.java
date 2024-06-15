package org.evrete.runtime;

import org.evrete.api.Copyable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class MapOfSet<K, V> extends MapOfCollection<K,V,Set<V>> implements Copyable<MapOfSet<K, V>> {
    private static final long serialVersionUID = 6435509932398742276L;

    public MapOfSet() {
        super(k -> new HashSet<>());
    }

    public <T> MapOfSet(Collection<T> collection, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        super(k -> new HashSet<>());
        this.addAll(collection, keyFunction, valueFunction);
    }

    @Override
    public MapOfSet<K, V> copyOf() {
        MapOfSet<K, V> m = new MapOfSet<>();
        this.forEach((k, vs) -> {
            for (V v : vs) {
                m.add(k, v);
            }
        });
        return m;
    }
}
