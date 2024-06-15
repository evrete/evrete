package org.evrete.runtime;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class MapOfList<K, V> extends MapOfCollection<K, V, List<V>> {
    private static final long serialVersionUID = 6435509532398742276L;
    private final Function<K, List<V>> newListFunction = k -> new LinkedList<>();

    public MapOfList() {
        super(k -> new LinkedList<>());
    }
}
