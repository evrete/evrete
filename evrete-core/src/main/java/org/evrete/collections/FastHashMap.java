package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class FastHashMap<K, V> extends AbstractFastHashMap<K, V> {

    @SuppressWarnings("unused")
    public FastHashMap() {
        super();
    }

    public FastHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected ToIntFunction<K> keyHashFunction() {
        return DEFAULT_HASH::applyAsInt;
    }

    @Override
    protected BiPredicate<K, K> keyHashEquals() {
        return DEFAULT_EQUALS::test;
    }
}
