package org.evrete.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface KeyReIterables<K> {

    EnumMap<KeyMode, ReIterable<K>> keyIterables();

    default KeyReIterators<K> keyIterators() {
        return keyIterators(k -> k);
    }

    default <Z> KeyReIterators<Z> keyIterators(Supplier<Function<K, Z>> mapperSupplier) {
        final EnumMap<KeyMode, ReIterator<Z>> map = new EnumMap<>(KeyMode.class);
        for (Map.Entry<KeyMode, ReIterable<K>> entry : keyIterables().entrySet()) {
            Function<K, Z> mapper = mapperSupplier.get();
            map.put(entry.getKey(), entry.getValue().iterator(mapper));
        }
        return () -> map;
    }

    default <Z> KeyReIterators<Z> keyIterators(Function<K, Z> mapper) {
        final EnumMap<KeyMode, ReIterator<Z>> map = new EnumMap<>(KeyMode.class);
        for (Map.Entry<KeyMode, ReIterable<K>> entry : keyIterables().entrySet()) {
            map.put(entry.getKey(), entry.getValue().iterator(mapper));
        }
        return () -> map;
    }
}
