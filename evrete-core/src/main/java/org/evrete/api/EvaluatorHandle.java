package org.evrete.api;

import java.io.Serializable;

/**
 * Evaluator handles essentially act as references to LHS (Left Hand Side) conditions, much like how each fact
 * in working memory is associated with a fact handle. While evaluator handles are mostly used under the hood,
 * advanced users may use these handles to replace conditions on the fly.
 */
public interface EvaluatorHandle extends Serializable {

}
