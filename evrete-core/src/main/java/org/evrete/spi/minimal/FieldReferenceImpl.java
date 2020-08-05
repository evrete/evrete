package org.evrete.spi.minimal;

import org.evrete.api.NamedType;
import org.evrete.api.TypeField;
import org.evrete.runtime.builder.FieldReference;

import java.util.Objects;

class FieldReferenceImpl implements FieldReference {
    private final NamedType type;
    private final TypeField field;

    FieldReferenceImpl(NamedType type, TypeField field) {
        this.type = type;
        this.field = field;
    }

    protected FieldReferenceImpl(FieldReference other) {
        this.type = other.type();
        this.field = other.field();
    }

    @Override
    public final TypeField field() {
        return field;
    }

    @Override
    public final NamedType type() {
        return type;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldReferenceImpl that = (FieldReferenceImpl) o;
        return type.equals(that.type) &&
                field.equals(that.field);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type, field);
    }
}
