package org.evrete.api;

import org.evrete.runtime.builder.FieldReference;

public interface Evaluator extends ValuesPredicate, LogicallyComparable, ComplexityObject {
    /**
     * Describes fields and their ordering to be used during the evaluation
     *
     * @return fields in correct order
     */
    FieldReference[] descriptor();
}
