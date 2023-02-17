package org.evrete.api;

/**
 * <p>
 * A unit of work with given complexity. The complexity can either be
 * computed as a sum of underlying complexity objects or assigned by user.
 * </p>
 * <p>
 * The complexity value is a key parameter for a set of
 * optimization tasks like grouping and sorting of conditions and for
 * building the Rete evaluation graph.
 * </p>
 */
public interface WorkUnit {
    double DEFAULT_COMPLEXITY = 1.0;

    default double getComplexity() {
        return DEFAULT_COMPLEXITY;
    }
}
