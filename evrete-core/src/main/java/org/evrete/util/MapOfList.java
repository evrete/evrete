package org.evrete.util;

import org.evrete.api.Copyable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MapOfList<K, V> extends HashMap<K, List<V>> implements Copyable<MapOfList<K, V>> {
    private static final long serialVersionUID = 6435509932398741276L;

    public void add(K k, V v) {
        computeIfAbsent(k, s -> new LinkedList<>()).add(v);
    }

    @Override
    public MapOfList<K, V> copyOf() {
        MapOfList<K, V> m = new MapOfList<>();
        this.forEach((k, vs) -> {
            for (V v : vs) {
                m.add(k, v);
            }
        });
        return m;
    }
}
