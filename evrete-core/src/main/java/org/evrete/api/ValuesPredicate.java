package org.evrete.api;

/**
 * <p>
 * A wrapper for predicate over {@link IntToValue}.
 * </p>
 */
@FunctionalInterface
public interface ValuesPredicate {

    boolean test(IntToValue t);
}
