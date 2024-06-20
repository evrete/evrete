package org.evrete.runtime.rete;


import org.evrete.runtime.evaluation.BetaEvaluator;

public class ReteKnowledgeConditionNode extends ReteKnowledgeNode {
    final ReteKnowledgeEvaluator evaluator;

    public ReteKnowledgeConditionNode(BetaEvaluator evaluator, ReteKnowledgeNode[] sourceNodes) {
        super(sourceNodes);
        this.evaluator = new ReteKnowledgeEvaluator(evaluator, this);
    }

    public ReteKnowledgeEvaluator getEvaluator() {
        return evaluator;
    }

}
