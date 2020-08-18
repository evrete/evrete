package org.evrete.runtime.aggregate;

import org.evrete.runtime.AbstractRuntimeLhs;
import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeAggregateLhsLoose;

import java.util.function.BooleanSupplier;

public class NotExistsEvaluatorFactory implements AggregateEvaluatorFactory {
    public static final NotExistsEvaluatorFactory INSTANCE = new NotExistsEvaluatorFactory();

    private NotExistsEvaluatorFactory() {
    }

    @Override
    public BooleanSupplier looseGroupEvaluator(RuntimeAggregateLhsLoose group) {
        return new NotExistsEvaluatorLoose(group);
    }

    @Override
    public BooleanSupplier joinedGroupEvaluator(AbstractRuntimeLhs root, RuntimeAggregateLhsJoined aggregate) {
        return new NotExistsEvaluatorJoined(root, aggregate);
    }
}
