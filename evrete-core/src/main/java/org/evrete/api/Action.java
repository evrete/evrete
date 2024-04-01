package org.evrete.api;

/**
 * The Action enum represents the actions that can be performed on a rule session.
 */
public enum Action {
    /**
     * The INSERT variable represents the action of inserting a fact.
     */
    INSERT,
    /**
     * Represents the action of updating a fact in a rule session.
     */
    UPDATE,
    /**
     * The RETRACT variable represents the action of retracting a fact.
     */
    RETRACT
}
