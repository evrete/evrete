package org.evrete.runtime.async;

import org.evrete.runtime.RuntimeAggregateLhsJoined;

import java.util.Collection;

public class AggregateComputeTask extends Completer {
    private final Collection<RuntimeAggregateLhsJoined> affectedAggregateNodes;

    public AggregateComputeTask(Collection<RuntimeAggregateLhsJoined> affectedAggregateNodes) {
        this.affectedAggregateNodes = affectedAggregateNodes;
    }

    @Override
    protected void execute() {
        tailCall(
                affectedAggregateNodes,
                m -> new RunnableCompleter(AggregateComputeTask.this, m::evaluate)
        );
    }


}
