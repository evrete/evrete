package org.evrete.api;

import java.util.function.Predicate;

/**
 * Represents a boolean-valued function of one {@code int}-valued
 * and one object-valued argument. This is the {@code int}-consuming primitive
 * specialization for {@link java.util.function.BiPredicate}.
 */
@FunctionalInterface
public interface IntObjectPredicate<Z> {
    /**
     * Evaluates a boolean-valued function on an integer and an object.
     *
     * @param i the integer argument to be tested
     * @param o the object argument to be tested
     * @return {@code true} if the function evaluates to {@code true}, otherwise {@code false}
     */
    boolean test(int i, Z o);

    /**
     * Combines this {@code IntObjectPredicate} with the specified {@code Predicate} to create a new {@code IntObjectPredicate}.
     * The resulting predicate evaluates to {@code true} if either this {@code IntObjectPredicate} or the specified {@code Predicate} evaluates to {@code true}.
     * Both the {@code int} argument and the object argument will be tested by this {@code IntObjectPredicate}.
     *
     * @param predicate the {@code Predicate} to be combined with this {@code IntObjectPredicate}
     * @return a new {@code IntObjectPredicate} that evaluates to {@code true} if either this {@code IntObjectPredicate} or the specified {@code Predicate} evaluates to {@code true
     * }
     */
    default IntObjectPredicate<Z> or(Predicate<Z> predicate) {
        return (i, z) -> test(i, z) || predicate.test(z);
    }
}
