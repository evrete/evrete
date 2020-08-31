package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class FastIdentityHashSet<K> extends AbstractFastHashSet<K> {

    public FastIdentityHashSet() {
        super();
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return IDENTITY_HASH;
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return IDENTITY_EQUALS;
    }
}
