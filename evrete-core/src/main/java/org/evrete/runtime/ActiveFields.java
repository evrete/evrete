package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.Copyable;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ActiveFields implements Copyable<ActiveFields> {
    private final Map<Type, TypeData> typeData = new HashMap<>();

    public ActiveFields() {
    }

    private ActiveFields(ActiveFields other) {
        for (Map.Entry<Type, TypeData> entry : other.typeData.entrySet()) {
            this.typeData.put(entry.getKey(), entry.getValue().copyOf());
        }
    }

    public synchronized ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
        return typeData.computeIfAbsent(field.getDeclaringType(), v -> new TypeData()).getCreate(field, listener);
    }

    public ActiveField[] getActiveFields(Type t) {
        TypeData d = typeData.get(t);
        return d == null ? ActiveField.ZERO_ARRAY : d.fieldsInUse;
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

        ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
            for (ActiveField af : fieldsInUse) {
                if (af.getDelegate().equals(field)) {
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
    }
}
