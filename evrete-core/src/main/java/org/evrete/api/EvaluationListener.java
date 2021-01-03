package org.evrete.api;

import org.evrete.runtime.ConditionNodeDescriptor;
import org.evrete.runtime.memory.BetaMemoryNode;

public interface EvaluationListener {

    void apply(BetaMemoryNode<ConditionNodeDescriptor> node, Evaluator evaluator, IntToValue values, boolean result);
}
