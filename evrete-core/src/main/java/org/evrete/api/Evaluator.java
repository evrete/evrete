package org.evrete.api;

import org.evrete.runtime.builder.FieldReference;

public interface Evaluator extends ValuesPredicate, LogicallyComparable, ComplexityObject {
    /**
     * <p>Describes fields and their ordering to be used during the evaluation</p>
     *
     * @return fields in correct order
     */
    FieldReference[] descriptor();

    /**
     * <p>A convenience method to convert evaluator's arguments to object array.</p>
     *
     * @param values evaluator's arguments as a functional inteface
     * @return arguments as an array
     */
    default Object[] toArray(IntToValue values) {
        Object[] array = new Object[descriptor().length];
        for (int i = 0; i < array.length; i++) {
            array[i] = values.apply(i);
        }
        return array;
    }

}
