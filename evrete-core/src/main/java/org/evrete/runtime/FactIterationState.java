package org.evrete.runtime;

import org.evrete.api.FactHandle;

//TODO !!!! reuse FactHandleTuple
public class FactIterationState {
    final TypeMemory typeMemory;
    FactHandle handle;
    Object value;

    FactIterationState(TypeMemory typeMemory) {
        this.typeMemory = typeMemory;
    }

    public FactHandle getHandle() {
        return handle;
    }


    public Object getFact() {
        return value;
    }
}
