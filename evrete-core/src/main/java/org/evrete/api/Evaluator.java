package org.evrete.api;

/**
 * An internal representation of every condition in the rule engine.
 */
public interface Evaluator extends ValuesPredicate {
    /**
     * Represents a constant value indicating no relation.
     */
    int RELATION_NONE = 0;

    /**
     * The RELATION_EQUALS variable represents a constant value indicating equality relation.
     */
    int RELATION_EQUALS = 1;

    /**
     * The RELATION_INVERSE variable represents a constant value indicating inverse relationship.
     */
    int RELATION_INVERSE = -1;

    /**
     * <p>Describes fields and their ordering to be used during the evaluation</p>
     *
     * @return fields in correct order
     */
    FieldReference[] descriptor();

    /**
     * @param other predicate
     * @return positive integer (&gt;0) if predicates are logically the same,
     * negative integer (&lt;0) if predicates are logically inverted  (like a == 2 and a != 2)
     * zero (0) if there is no knowledge about the two or if they are independent
     */
    default int compare(Evaluator other) {
        if (this.equals(other)) {
            return RELATION_EQUALS;
        } else {
            return RELATION_NONE;
        }
    }


    /**
     * <p>A convenience method to convert evaluator's arguments to object array.</p>
     *
     * @param values evaluator's arguments as a functional interface
     * @return arguments as an array
     */
    @SuppressWarnings("unused")
    default Object[] toArray(IntToValue values) {
        Object[] array = new Object[descriptor().length];
        for (int i = 0; i < array.length; i++) {
            array[i] = values.apply(i);
        }
        return array;
    }

    /**
     * Checks if the descriptor of this evaluator is the same as the descriptor of another evaluator.
     *
     * @param other the other evaluator to compare descriptors with
     * @return true if the descriptors of the two evaluators are the same, false otherwise
     */
    default boolean sameDescriptor(Evaluator other) {
        return FieldReference.sameAs(descriptor(), other.descriptor());
    }
}
