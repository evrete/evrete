package org.evrete.api;


public interface MemoryKey extends FieldToValueHandle {

    ValueHandle get(int fieldIndex);

    int getMetaValue();

    void setMetaValue(int i);

    @Override
    default ValueHandle apply(ActiveField activeField) {
        return get(activeField.getValueIndex());
    }
}
