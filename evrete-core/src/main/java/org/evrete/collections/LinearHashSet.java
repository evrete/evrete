package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class LinearHashSet<K> extends AbstractLinearHashSet<K> {

    public LinearHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public LinearHashSet() {
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
