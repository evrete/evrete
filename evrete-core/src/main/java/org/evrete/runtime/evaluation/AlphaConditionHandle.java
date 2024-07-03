package org.evrete.runtime.evaluation;

import org.evrete.api.WorkUnit;
import org.evrete.runtime.PreHashed;
import org.evrete.util.Indexed;

/**
 * Indexed version of an alpha condition (predicate on a single fact). This handle wrapper is identified
 * by an index that is unique inside a {@link org.evrete.runtime.ActiveType}
 * We need indexed alpha conditions to be used inside a {@link org.evrete.runtime.Mask},
 * where each bit represents the success or failure of the corresponding fact predicate.
 */
public class AlphaConditionHandle extends PreHashed implements WorkUnit, Indexed {
    private final DefaultEvaluatorHandle handle;
    private final int index;

    /**
     * Constructor to create an AlphaConditionHandle.
     *
     * @param index the index assigned to the handle within the type
     * @param handle the handle
     */
    public AlphaConditionHandle(int index, DefaultEvaluatorHandle handle) {
        super(handle.getIndex());
        this.handle = handle;
        this.index = index;
    }

    public DefaultEvaluatorHandle getHandle() {
        return handle;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public double getComplexity() {
        return handle.getComplexity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlphaConditionHandle that = (AlphaConditionHandle) o;
        return handle.getIndex() == that.handle.getIndex();
    }
}
