package org.evrete.api;

import java.util.EnumMap;

public interface KeyIteratorsBundle<K> {

    EnumMap<KeyMode, ReIterator<K>> keyIterators();
}
