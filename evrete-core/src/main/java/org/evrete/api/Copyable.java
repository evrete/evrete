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
//TODO drop the class
public interface Copyable<T> {
    T copyOf();
}
