package org.evrete.api;

/**
 * Represents a literal {@link Evaluator} with its source code.
 */
public interface LiteralEvaluator extends Evaluator {
    LiteralExpression getSource();
}
