package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class LinearIdentityHashMap<K, V> extends AbstractLinearHashMap<K, V> {
    public LinearIdentityHashMap() {
        super();
    }

    public LinearIdentityHashMap(int initialCapacity) {
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
