package org.evrete.dsl;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleSession;

/**
 * <p>
 * An event variable that binds listener methods to a specific lifecycle event:
 * </p>
 * <ul>
 *     <li>
 *         <code>BUILD</code> event happens when a new {@link org.evrete.api.Knowledge} is created from Java
 *         sources, classes or archives. Unlike the rest of the events, the <code>BUILD</code> event can be
 *         referenced by <strong>static</strong> methods only.
 *     </li>
 *     <li>
 *         <code>CREATE</code> event happens before a new session is created off the given ruleset, namely
 *         before the {@link Knowledge#createSession()} is called.
 *     </li>
 *     <li>
 *         <code>FIRE</code> event happens before any of the {@link RuleSession#fire()}, {@link RuleSession#fireAsync(Object)},
 *         or {@link RuleSession#fireAsync()} are called on a session instance.
 *     </li>
 *     <li>
 *         <code>CLOSE</code> event happens right before the session's {@link RuleSession#close()} method is called.
 *     </li>
 * </ul>
 */
public enum Phase {
    /**
     * @see Phase
     */
    BUILD,
    /**
     * @see Phase
     */
    CREATE,
    /**
     * @see Phase
     */
    FIRE,
    /**
     * @see Phase
     */
    CLOSE
}
