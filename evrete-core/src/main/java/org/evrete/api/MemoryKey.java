package org.evrete.api;

/**
 * This interface represents a multi-value key used for accessing values from memory. It provides methods
 * to get and set the meta value associated with the key, as well as retrieve a specific value
 * from the key.
 */
public interface MemoryKey {

    /**
     * Retrieves a ValueHandle associated with the specified field index.
     *
     * @param fieldIndex the index of the field
     * @return the ValueHandle associated with the field
     */
    ValueHandle get(int fieldIndex);

    /**
     * Retrieves the meta value associated with this MemoryKey.
     *
     * @return the meta value
     */
    int getMetaValue();

    /**
     * Sets the meta value associated with this MemoryKey.
     *
     * @param i the new meta value to set
     */
    void setMetaValue(int i);
}
