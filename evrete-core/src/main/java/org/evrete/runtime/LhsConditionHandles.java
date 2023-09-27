package org.evrete.runtime;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

class LhsConditionHandles {
    private final Collection<EvaluatorHandle> handles = new LinkedList<>();

    public Collection<EvaluatorHandle> getHandles() {
        return handles;
    }

    public void add(@NonNull EvaluatorHandle handle) {
        this.handles.add(Objects.requireNonNull(handle));
    }
}
