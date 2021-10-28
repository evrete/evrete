package org.evrete.api;

import java.util.function.Predicate;

@FunctionalInterface
public interface IntObjectPredicate<Z> {
    boolean test(int i, Z o);

    default IntObjectPredicate<Z> or(Predicate<Z> predicate) {
        return (i, z) -> test(i, z) || predicate.test(z);
    }
}
