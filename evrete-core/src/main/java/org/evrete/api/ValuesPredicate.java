package org.evrete.api;

/**
 * Every condition in the engine is a {@link ValuesPredicate}. When the condition's parameters are counted
 * and resolved to fields, the engine will call {@link IntToValue#get(int)} to obtain each field's value.
 * <p>
 * Additionally, using the {@link IntToValue} instead of arrays allows for index mapping
 * in scenarios where the required values are held in different value structures. Without {@link IntToValue},
 * we would need to create a new <code>Object[]</code> array for each evaluation.
 * </p>
 */
@FunctionalInterface
public interface ValuesPredicate {

    boolean test(IntToValue t);
}
