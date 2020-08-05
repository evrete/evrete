package org.evrete.runtime.builder;

import org.evrete.api.FactBuilder;
import org.evrete.api.FactSelector;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.aggregate.AggregateEvaluatorFactory;
import org.evrete.runtime.aggregate.ExistsEvaluatorFactory;
import org.evrete.runtime.aggregate.NotExistsEvaluatorFactory;

public class AggregateLhsBuilder<C extends RuntimeContext<C, ?>> extends LhsBuilder<C, AggregateLhsBuilder<C>> implements FactSelector<AggregateLhsBuilder<C>> {
    private final RootLhsBuilder<C> parent;
    private AggregateEvaluatorFactory aggregateEvaluatorFactory;

    AggregateLhsBuilder(RootLhsBuilder<C> parent) {
        super(parent);
        this.parent = parent;
    }


    public AggregateLhsBuilder<C> forEach(FactBuilder... facts) {
        return buildLhs(facts);
    }

    public RootLhsBuilder<C> exists() {
        this.aggregateEvaluatorFactory = ExistsEvaluatorFactory.INSTANCE;
        parent.saveAggregate(this);
        return parent;
    }

    public RootLhsBuilder<C> notExists() {
        this.aggregateEvaluatorFactory = NotExistsEvaluatorFactory.INSTANCE;
        parent.saveAggregate(this);
        return parent;
    }

    public AggregateEvaluatorFactory getAggregateEvaluatorFactory() {
        return aggregateEvaluatorFactory;
    }

    @Override
    protected AggregateLhsBuilder<C> self() {
        return this;
    }

}
