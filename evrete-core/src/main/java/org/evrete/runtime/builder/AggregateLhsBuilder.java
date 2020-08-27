package org.evrete.runtime.builder;

import org.evrete.api.FactBuilder;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.aggregate.AggregateEvaluatorFactory;
import org.evrete.runtime.aggregate.ExistsEvaluatorFactory;
import org.evrete.runtime.aggregate.NotExistsEvaluatorFactory;

public class AggregateLhsBuilder<C extends RuntimeContext<C>> extends AbstractLhsBuilder<C, AggregateLhsBuilder<C>> {
    private final LhsBuilder<C> parent;
    private AggregateEvaluatorFactory aggregateEvaluatorFactory;

    AggregateLhsBuilder(LhsBuilder<C> parent, FactBuilder[] facts) {
        super(parent);
        if(facts == null || facts.length == 0) {
            throw new IllegalArgumentException("Empty fact selection in a sub-query");
        }
        this.parent = parent;
        buildLhs(facts);
    }

    public LhsBuilder<C> exists() {
        this.aggregateEvaluatorFactory = ExistsEvaluatorFactory.INSTANCE;
        parent.saveAggregate(this);
        return parent;
    }

    public LhsBuilder<C> notExists() {
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
