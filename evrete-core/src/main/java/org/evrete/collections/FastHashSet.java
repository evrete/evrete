package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class FastHashSet<K> extends AbstractFastHashSet<K> {

    public FastHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public FastHashSet() {
        super();
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return DEFAULT_HASH;
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return DEFAULT_EQUALS;
    }
}
