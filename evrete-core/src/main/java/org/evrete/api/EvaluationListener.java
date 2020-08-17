package org.evrete.api;

import org.evrete.runtime.memory.BetaConditionNode;

public interface EvaluationListener {

    void apply(BetaConditionNode node, Evaluator evaluator, IntToValue values, boolean result);
}
