package org.evrete.runtime;

import org.evrete.api.Copyable;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.collections.ArrayOf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class RuntimeMetaData implements Copyable<RuntimeMetaData> {
    //TODO !!! move to type meta data
    private final Set<String> imports;
    private final ArrayOf<TypeMeta> typeMetas;

    RuntimeMetaData() {
        this.imports = new HashSet<>();
        this.typeMetas = new ArrayOf<>(TypeMeta.class);
    }

    private RuntimeMetaData(RuntimeMetaData other) {
        this.imports = new HashSet<>(other.imports);
        this.typeMetas = new ArrayOf<>(TypeMeta.class);
        other.typeMetas
                .forEach(
                        (meta, i) -> RuntimeMetaData.this.typeMetas.set(i, meta.copyOf())
                );
    }

    private TypeMeta get(Type<?> type) {
        return typeMetas.computeIfAbsent(type.getId(), k -> new TypeMeta());
    }

    ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
        return get(field.getDeclaringType()).getCreate(field, listener);
    }

    ActiveField[] getActiveFields(Type<?> t) {
        return get(t).activeFields;
    }

    void addImport(String imp) {
        if (imp != null && !imp.isEmpty()) {
            this.imports.add(imp);
        }
    }

    Set<String> getImports() {
        return imports;
    }

    @Override
    public RuntimeMetaData copyOf() {
        return new RuntimeMetaData(this);
    }


    private static class TypeMeta implements Copyable<TypeMeta> {
        private ActiveField[] activeFields;


        TypeMeta() {
            this.activeFields = ActiveField.ZERO_ARRAY;
        }

        TypeMeta(TypeMeta other) {
            this.activeFields = Arrays.copyOf(other.activeFields, other.activeFields.length);
        }

        @Override
        public TypeMeta copyOf() {
            return new TypeMeta(this);
        }

        private synchronized ActiveField getCreate(TypeField field, Consumer<ActiveField> listener) {
            for (ActiveField af : activeFields) {
                if (af.getName().equals(field.getName())) {
                    return af;
                }
            }
            // Create and store new instance
            int id = activeFields.length;
            ActiveField af = new ActiveField(field, id);
            this.activeFields = Arrays.copyOf(this.activeFields, id + 1);
            this.activeFields[id] = af;
            listener.accept(af);
            return af;
        }

    }
}
