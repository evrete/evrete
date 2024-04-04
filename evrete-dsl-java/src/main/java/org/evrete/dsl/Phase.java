package org.evrete.dsl;

/**
 * <p>
 * An event variable that binds listener methods to a specific lifecycle event:
 * </p>
 * <ul>
 *     <li>
 *         {@code BUILD} event happens when a new {@link org.evrete.api.Knowledge} is created from Java
 *         sources, classes or archives. Unlike the rest of the events, the {@code BUILD} event can be
 *         referenced by <strong>static</strong> methods only.
 *     </li>
 *     <li>
 *         {@code CREATE} event happens immediately after a new session is created off the given ruleset
 *     </li>
 *     <li>
 *         {@code FIRE} event happens before any of the {@link org.evrete.api.StatefulSession#fire()}, {@link org.evrete.api.StatefulSession#fireAsync(Object)},
 *         or {@link org.evrete.api.StatefulSession#fireAsync()} are called on a session instance.
 *     </li>
 *     <li>
 *         {@code CLOSE} event happens right before the session's {@link org.evrete.api.StatefulSession#close()} method is called.
 *     </li>
 * </ul>
 */
public enum Phase {
    /**
     * This event is fired when a new {@link org.evrete.api.Knowledge} is created from Java
     * sources, classes or archives.
     */
    BUILD,

    /**
     * Fired immediately after a new session is created off the given ruleset.
     */
    CREATE,

    /**
     * Fired before any of the {@link org.evrete.api.StatefulSession#fire()}, {@link org.evrete.api.StatefulSession#fireAsync(Object)},
     * or {@link org.evrete.api.StatefulSession#fireAsync()} are called on a session instance.
     */
    FIRE,

    /**
     * Fired right before the session's {@link org.evrete.api.StatefulSession#close()} method is called.
     */
    CLOSE
}
