package org.evrete.runtime;

import java.util.LinkedList;
import java.util.List;

public class MapOfList<K, V> extends MapOfCollection<K, V, List<V>> {
    private static final long serialVersionUID = 6435509532398742276L;

    public MapOfList() {
        super(k -> new LinkedList<>());
    }
}
