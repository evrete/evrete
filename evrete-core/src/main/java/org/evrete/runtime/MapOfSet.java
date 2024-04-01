package org.evrete.runtime;

import org.evrete.api.Copyable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class MapOfSet<K, V> extends HashMap<K, Set<V>> implements Copyable<MapOfSet<K, V>> {
    private static final long serialVersionUID = 6435509932398742276L;
    private final Function<K, Set<V>> newSetFunction = k -> new HashSet<>();

    public void add(K k, V v) {
        computeIfAbsent(k, newSetFunction).add(v);
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
