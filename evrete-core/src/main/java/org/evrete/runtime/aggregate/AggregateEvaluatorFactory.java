package org.evrete.runtime.aggregate;

import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeAggregateLhsLoose;
import org.evrete.runtime.RuntimeLhs;

import java.util.function.BooleanSupplier;

public interface AggregateEvaluatorFactory {

    BooleanSupplier looseGroupEvaluator(RuntimeAggregateLhsLoose group);

    BooleanSupplier joinedGroupEvaluator(RuntimeLhs root, RuntimeAggregateLhsJoined aggregate);

}
