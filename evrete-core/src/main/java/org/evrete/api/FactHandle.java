package org.evrete.api;

import java.io.Serializable;

/**
 * Represents a unique identifier for a fact in working memory.
 */
public interface FactHandle extends Serializable {

    /**
     * Returns the unique identifier of the fact.
     *
     * @return the unique identifier.
     */
    long getId();
}
