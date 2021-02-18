/*
package org.evrete.runtime;

import org.evrete.api.Copyable;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.collections.ArrayOf;

import java.util.Arrays;
import java.util.function.Consumer;

public class ActiveFields implements Copyable<ActiveFields> {
    private final ArrayOf<TypeData> typeData;

    ActiveFields() {
        this.typeData = new ArrayOf<>(TypeData.class);
    }

    private ActiveFields(ActiveFields other) {
        // Copying array
        this.typeData = new ArrayOf<>(other.typeData);
        for (int i = 0; i < this.typeData.data.length; i++) {
            TypeData otherData = other.typeData.get(i);
            if (otherData != null) {
                this.typeData.set(i, otherData.copyOf());
            }
        }
    }

    synchronized ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
        int id = field.getDeclaringType().getId();
        TypeData td = this.typeData.get(id);
        if (td == null) {
            td = new TypeData();
            this.typeData.set(id, td);
        }
        return td.getCreate(field, listener);
    }

    ActiveField[] getActiveFields(Type<?> t) {
        TypeData d = typeData.get(t.getId());
        return d == null ? ActiveField.ZERO_ARRAY : d.fieldsInUse;
    }

    @Override
    public String toString() {
        return "ActiveFields{" +
                "typeData=" + typeData +
                '}';
    }

    @Override
    public ActiveFields copyOf() {
        return new ActiveFields(this);
    }

    private static class TypeData implements Copyable<TypeData> {
        private ActiveField[] fieldsInUse = ActiveField.ZERO_ARRAY;

        TypeData() {
        }

        TypeData(TypeData other) {
            this.fieldsInUse = Arrays.copyOf(other.fieldsInUse, other.fieldsInUse.length);
        }

        private ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
            for (ActiveField af : fieldsInUse) {
                if (af.getName().equals(field.getName())) {
                    return af;
                }
            }
            // Create and store new instance
            ActiveField af = new ActiveField(field, fieldsInUse.length);
            this.fieldsInUse = Arrays.copyOf(this.fieldsInUse, this.fieldsInUse.length + 1);
            this.fieldsInUse[af.getValueIndex()] = af;
            listener.accept(af);
            return af;
        }

        @Override
        public TypeData copyOf() {
            return new TypeData(this);
        }

        @Override
        public String toString() {
            return "TypeData{" +
                    "fieldsInUse=" + Arrays.toString(fieldsInUse) +
                    '}';
        }
    }
}
*/
