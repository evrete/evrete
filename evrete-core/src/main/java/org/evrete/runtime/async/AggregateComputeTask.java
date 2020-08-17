package org.evrete.runtime.async;

import org.evrete.runtime.RuntimeAggregateLhsJoined;

import java.util.Collection;

public class AggregateComputeTask extends Completer {
    private final Collection<RuntimeAggregateLhsJoined> affectedAggregateNodes;
    private final boolean deltaOnly;

    public AggregateComputeTask(Collection<RuntimeAggregateLhsJoined> affectedAggregateNodes, boolean deltaOnly) {
        this.affectedAggregateNodes = affectedAggregateNodes;
        this.deltaOnly = deltaOnly;
    }

    @Override
    protected void execute() {
        tailCall(
                affectedAggregateNodes,
                m -> new RunnableCompleter(AggregateComputeTask.this, () -> m.evaluate(deltaOnly))
        );
    }


}
