/*
package org.evrete.runtime;

import java.util.function.BooleanSupplier;

public class RuntimeAggregateLhsLoose extends RuntimeAggregateLhs implements BooleanSupplier {
    private final BooleanSupplier lhsEvaluator;


    public RuntimeAggregateLhsLoose(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AggregateLhsDescriptor descriptor) {
        super(rule, parent, descriptor);
        this.lhsEvaluator = descriptor.getAggregateEvaluatorFactory().looseGroupEvaluator(this);
    }

    @Override
    public boolean getAsBoolean() {
        return lhsEvaluator.getAsBoolean();
    }
}
*/
