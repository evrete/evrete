package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class LinearHashMap<K, V> extends AbstractLinearHashMap<K, V> {

    @SuppressWarnings("unused")
    public LinearHashMap() {
        super();
    }

    public LinearHashMap(int initialCapacity) {
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
