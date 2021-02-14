package org.evrete.api;

import java.io.Serializable;

/**
 * <p>
 * FactHandle instances uniquely identify working instances in rule sessions. To ensure that, FactHandle implementations
 * are expected to be serializable and have both hashCode() end equals() methods implemented.
 * </p>
 */
public interface FactHandle extends Serializable {

    /**
     * <p>
     * Returns the id of the <code>Type</code> which inserted instance was initially associated with.
     * </p>
     *
     * @return the id of the associated instance type
     * @see Type#getId()
     */
    int getTypeId();
}
