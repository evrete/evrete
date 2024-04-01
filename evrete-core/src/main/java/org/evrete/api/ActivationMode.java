package org.evrete.api;

/**
 * Enum representing the activation mode for a session.
 */
public enum ActivationMode {
    /**
     * With this mode, the engine executes all the rules on the agenda and
     * collects all the WMA (Working Memory Action) incurred by rules' actions.
     * Once the engine reaches the end of the agenda, the collected memory actions
     * become a new WMA buffer.
     * With this strategy, neither of the rules on the agenda is aware of the
     * WMAs created by previous rules.
     */
    CONTINUOUS,
    /**
     * This mode employs an internal versioning mechanism that allows a rule's
     * action block to see changes made by the preceding rules on the agenda.
     * Facts that are marked for deletion or have a different version are excluded
     * from the action block.
     */
    DEFAULT
}
