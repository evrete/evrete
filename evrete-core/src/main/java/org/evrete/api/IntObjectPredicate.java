package org.evrete.api;

import java.util.function.Predicate;

/**
 * Represents a boolean-valued function of one {@code int}-valued
 * and one object-valued argument. This is the {@code int}-consuming primitive
 * specialization for {@link java.util.function.BiPredicate}.
 */
@FunctionalInterface
public interface IntObjectPredicate<Z> {
    boolean test(int i, Z o);

    default IntObjectPredicate<Z> or(Predicate<Z> predicate) {
        return (i, z) -> test(i, z) || predicate.test(z);
    }
}
