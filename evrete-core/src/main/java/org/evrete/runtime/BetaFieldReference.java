package org.evrete.runtime;

import org.evrete.api.TypeField;

public final class BetaFieldReference {
    private final FactType factType;
    //TODO !!!! unused var 'fieldIndex'
    private final int fieldIndex;
    private final ActiveField activeField;

    public BetaFieldReference(FactType factType, TypeField field) {
        this.factType = factType;
        this.fieldIndex = factType.findFieldPosition(field);
        this.activeField = factType.getMemoryAddress().fields().getFields()[this.fieldIndex];
    }

    public FactType getFactType() {
        return factType;
    }


    public ActiveField getActiveField() {
        return activeField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetaFieldReference that = (BetaFieldReference) o;
        return fieldIndex == that.fieldIndex &&
                factType.equals(that.factType);
    }

    @Override
    public int hashCode() {
        return factType.hashCode() * 31 + fieldIndex;
    }
}
