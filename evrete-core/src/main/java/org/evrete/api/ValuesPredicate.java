package org.evrete.api;

import java.util.function.Predicate;

/**
 * <p>
 * A wrapper for predicate over <code>IntToValue</code>.
 * </p>
 */
@FunctionalInterface
public interface ValuesPredicate extends Predicate<IntToValue> {

    default boolean test(final Object[] array) {
        return test(IntToValue.of(array));
    }
}
