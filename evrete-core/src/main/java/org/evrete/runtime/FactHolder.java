package org.evrete.runtime;


/**
 * A wrapper for a fact. The engine stores facts along with their field values and
 * the assigned {@link org.evrete.api.FactHandle}.
 */
public class FactHolder {
    private final FactFieldValues values;
    private final Object fact;
    private final DefaultFactHandle handle;

    /**
     * Constructs a new wrapping object.
     *
     * @param handle the assigned fact handle.
     * @param values the field values of the fact.
     * @param fact the original fact object.
     */
    public FactHolder(DefaultFactHandle handle, FactFieldValues values, Object fact) {
        this.values = values;
        this.fact = fact;
        this.handle = handle;
    }

    public DefaultFactHandle getHandle() {
        return handle;
    }

    public Object getFact() {
        return fact;
    }

    public FactFieldValues getValues() {
        return values;
    }


    @Override
    public String toString() {
        return "{fact=" + fact +
                ", values=" + values +
                '}';
    }
}

