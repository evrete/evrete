package org.evrete.runtime.aggregate;

import org.evrete.runtime.AbstractRuntimeLhs;
import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeAggregateLhsLoose;

import java.util.function.BooleanSupplier;

public class ExistsEvaluatorFactory implements AggregateEvaluatorFactory {
    public static final ExistsEvaluatorFactory INSTANCE = new ExistsEvaluatorFactory();

    private ExistsEvaluatorFactory() {
    }

    @Override
    public BooleanSupplier looseGroupEvaluator(RuntimeAggregateLhsLoose group) {
        return new ExistsEvaluatorLoose(group);
    }

    @Override
    public BooleanSupplier joinedGroupEvaluator(AbstractRuntimeLhs root, RuntimeAggregateLhsJoined aggregate) {
        return new ExistsEvaluatorJoined(root, aggregate);
    }
}
