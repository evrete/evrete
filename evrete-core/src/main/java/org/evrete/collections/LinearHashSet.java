package org.evrete.collections;

import java.util.Objects;
import java.util.function.BiPredicate;

public class LinearHashSet<K> extends AbstractLinearHashSet<K> {

    public LinearHashSet(int minimalCapacity) {
        super(minimalCapacity);
    }

    @Override
    protected BiPredicate<K, K> getEqualsPredicate() {
        return Objects::equals;
    }
}
