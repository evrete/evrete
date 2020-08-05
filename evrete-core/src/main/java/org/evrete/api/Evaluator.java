package org.evrete.api;

import org.evrete.runtime.builder.FieldReference;

public interface Evaluator extends ValuesPredicate, LogicallyComparable, ComplexityObject {
    /**
     * Describes fields and their ordering to be used during the evaluation
     *
     * @return fields in correct order
     */
    FieldReference[] descriptor();

    default Evaluator withComplexity(double complexity) {
        Evaluator self = this;
        return new Evaluator() {
            @Override
            public FieldReference[] descriptor() {
                return self.descriptor();
            }

            @Override
            public double getComplexity() {
                return complexity;
            }

            @Override
            public boolean test(IntToValue values) {
                return self.test(values);
            }

            @Override
            public int compare(LogicallyComparable other) {
                return self.compare(other);
            }
        };
    }
}
