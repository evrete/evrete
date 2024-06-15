package org.evrete.api;

/**
 * A generic alternative to Cloneable. This interface provides a mechanism
 * for creating new branches of a data structure, similar to Git's branching
 * mechanism. Particularly in the engine, creating branches ensures that:
 * <ul>
 *   <li>All the data from the parent context (a {@link Knowledge}) is available to spawned sessions.</li>
 *   <li>Changes to the branched data (such as adding new rules to {@link RuleSession} instances) do not affect
 *       the parent {@link Knowledge} context.</li>
 * </ul>
 *
 * @param <T> the type of object to be branched
 */

public interface Branchable<T> {

    /**
     * Creates a new branch of the current instance, ensuring that changes to the new branch
     * do not impact the original instance. This method is similar to creating a branch in a
     * version control system, where modifications in the new branch are isolated from the parent.
     *
     * @return a new branch of the original data
     */
    T newBranch();
}
