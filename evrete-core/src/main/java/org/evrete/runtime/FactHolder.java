package org.evrete.runtime;


import org.evrete.api.annotations.NonNull;

import java.util.Objects;

/**
 * A wrapper for a fact. The engine stores facts along with their field values and
 * the assigned {@link org.evrete.api.FactHandle}.
 */
public class FactHolder {
    private final Object fact;
    private final DefaultFactHandle handle;
    private final long fieldValuesId;

    /**
     * Constructs a new wrapping object.
     *
     * @param handle the assigned fact handle.
     * @param fieldValuesId the unique id assigned to the combination of fact's field values.
     * @param fact the original fact object.
     */
    public FactHolder(@NonNull DefaultFactHandle handle, long fieldValuesId, @NonNull Object fact) {
        this.handle = Objects.requireNonNull(handle);
        this.fieldValuesId = fieldValuesId;
        this.fact = Objects.requireNonNull(fact);
    }


    public long getFieldValuesId() {
        return fieldValuesId;
    }

    public DefaultFactHandle getHandle() {
        return handle;
    }

    public Object getFact() {
        return fact;
    }


    @Override
    public String toString() {
        return "{fact=" + fact +
                ", values=" + fieldValuesId +
                '}';
    }
}

