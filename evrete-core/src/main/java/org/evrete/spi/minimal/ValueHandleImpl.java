package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;

import java.util.Objects;

class ValueHandleImpl implements ValueHandle {
    private static final long serialVersionUID = 4019004009002813739L;
    final int typeId;
    final int valueId;
    private final int hash;


    ValueHandleImpl(int typeId, int valueId) {
        this.typeId = typeId;
        this.valueId = valueId;
        this.hash = Objects.hash(typeId, valueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueHandleImpl handle = (ValueHandleImpl) o;
        return handle.valueId == valueId && handle.typeId == typeId;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return typeId + "/" + valueId;
    }
}
