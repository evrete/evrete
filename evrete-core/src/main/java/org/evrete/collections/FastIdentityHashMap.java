package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class FastIdentityHashMap<K, V> extends AbstractFastHashMap<K, V> {
    public FastIdentityHashMap() {
        super();
    }

    public FastIdentityHashMap(int initialCapacity) {
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
