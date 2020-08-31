package org.evrete.api;

import java.util.EnumMap;

public interface KeyReIterators<K> {

    EnumMap<KeyMode, ReIterator<K>> keyIterators();

    default ReIterator<K> keyIterator(KeyMode mode) {
        return keyIterators().get(mode);
    }
}
