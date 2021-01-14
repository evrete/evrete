package org.evrete.api;

/**
 * <p>
 * A wrapper for predicate over <code>IntToValue</code>.
 * </p>
 */
@FunctionalInterface
public interface ValuesPredicate {

    boolean test(IntToValue t);

    default boolean test(final Object[] array) {
        return test(IntToValue.of(array));
    }
}
