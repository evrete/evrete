package org.evrete.runtime.evaluation;

import org.evrete.runtime.memory.BetaConditionNode;
import org.evrete.runtime.memory.NodeIterationStateFactory;

public class DefaultStateFactory implements NodeIterationStateFactory<DefaultIterationState> {

    @Override
    public DefaultIterationState newIterationState(BetaConditionNode node) {
        return new DefaultIterationState(node);
    }

}
