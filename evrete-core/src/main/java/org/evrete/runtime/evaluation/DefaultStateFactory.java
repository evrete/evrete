package org.evrete.runtime.evaluation;

import org.evrete.runtime.BetaConditionNode;
import org.evrete.util.NodeIterationStateFactory;

public class DefaultStateFactory implements NodeIterationStateFactory<DefaultIterationState> {

    @Override
    public DefaultIterationState newIterationState(BetaConditionNode node) {
        return new DefaultIterationState(node);
    }

}
