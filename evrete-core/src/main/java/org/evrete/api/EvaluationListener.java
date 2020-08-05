package org.evrete.api;

import org.evrete.runtime.BetaEvaluationContext;
import org.evrete.runtime.memory.BetaConditionNode;

public interface EvaluationListener {

    void apply(BetaEvaluationContext ctx, BetaConditionNode node, Evaluator evaluator, IntToValue values, boolean result);
}
