package org.evrete.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Evaluator handles essentially act as references to LHS (Left Hand Side) conditions, much like how each fact in working memory is associated with a fact handle.
 * While evaluator handles are mostly used under the hood, advanced users may utilize these handles to replace conditions on the fly.
 */
public interface EvaluatorHandle extends WorkUnit {

    /**
     * @return an array of {@link FieldReference} objects representing the descriptor of this evaluator.
     */
    FieldReference[] descriptor();

    default Set<NamedType> namedTypes() {
        Set<NamedType> set = new HashSet<>();
        for (FieldReference r : descriptor()) {
            set.add(r.type());
        }
        return set;
    }
}
