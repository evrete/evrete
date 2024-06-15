package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;

/**
 * A convenience wrapper class used when iterating Rete memories.
 */
public class FactValuesReference {
    public final FactType factType;
    public final MemoryScope scope;
    public final FactFieldValues fieldValues;

    /**
     * Constructor for FactValuesReference.
     *
     * @param factType the type of the fact
     * @param scope the scope of the memory
     * @param fieldValues the values of the fact fields
     */
    public FactValuesReference(FactType factType, MemoryScope scope, FactFieldValues fieldValues) {
        this.factType = factType;
        this.scope = scope;
        this.fieldValues = fieldValues;
    }
}
