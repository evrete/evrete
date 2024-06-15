package org.evrete.runtime;

import java.util.Arrays;
import java.util.Objects;

public final class FieldsKey {
    private final int typeIndex;
    private final int[] activeFieldIndices;
    //private final int id;

    public FieldsKey(int typeIndex, int[] activeFieldIndices) {
        //this.id = id;
        this.typeIndex = typeIndex;
        this.activeFieldIndices = activeFieldIndices;
    }

/*
    public FieldsKey(int typeIndex, ActiveField[] activeFields) {
        //this.id = id;
        this.typeIndex = typeIndex;
        this.activeFieldIndices = new int[activeFields.length];
        for (int i = 0; i < activeFields.length; i++) {
            this.activeFieldIndices[i] = activeFields[i].getIndex();
        }
    }
*/

    public int getId() {
        throw new UnsupportedOperationException();
    }

    //TODO rename getter
    public int typeIndex() {
        return typeIndex;
    }

    public int size() {
        return activeFieldIndices.length;
    }

    public int[] activeFieldIndices() {
        return activeFieldIndices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldsKey fieldsKey = (FieldsKey) o;
        return typeIndex == fieldsKey.typeIndex && Objects.deepEquals(activeFieldIndices, fieldsKey.activeFieldIndices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeIndex, Arrays.hashCode(activeFieldIndices));
    }
}
