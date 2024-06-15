package org.evrete.api;

import org.evrete.api.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Base interface for each rule in the engine.
 * Provides mechanisms for getting and setting rule characteristics and behavior.
 */
public interface Rule extends Environment, Named, NamedType.Resolver {


    /**
     * Gets the RHS (Right Hand Side) logic as a Consumer.
     *
     * @return the RHS logic consumer.
     */
    Consumer<RhsContext> getRhs();

    /**
     * Sets the RHS logic using a literal string.
     * The literal will be compiled and converted into a Consumer.
     *
     * @param literalRhs the string representation of RHS logic.
     */
    void setRhs(String literalRhs);

    /**
     * Sets the RHS logic using a Consumer. Can be null.
     *
     * @param rhs the RHS logic consumer.
     */
    void setRhs(@Nullable Consumer<RhsContext> rhs);

    /**
     * Retrieves the rule's salience.
     *
     * @return the salience value.
     */
    int getSalience();

    /**
     * Sets the rule's salience.
     *
     * @param value the salience value to set.
     */
    void setSalience(int value);

    /**
     * Chains a consumer to the current RHS logic. The provided consumer is executed after the current RHS.
     *
     * @param consumer the consumer to chain.
     */
    void chainRhs(Consumer<RhsContext> consumer);

    /**
     * Sets a new name for the rule.
     *
     * @param newName the new name to give to the rule.
     */
    void setName(String newName);
}
