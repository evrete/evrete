package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.function.Function;

class TypeFieldImpl implements TypeField {
    private final String name;
    private final Class<?> valueType;
    private final TypeImpl<?> declaringType;
    private Function<Object, ?> function;

    TypeFieldImpl(TypeImpl<?> declaringType, String name, Class<?> valueType, Function<Object, ?> function) {
        this.name = name;
        this.valueType = valueType;
        this.function = function;
        this.declaringType = declaringType;
    }

    TypeFieldImpl(TypeFieldImpl other, TypeImpl<?> newType) {
        this(newType, other.name, other.valueType, other.function);
    }

    public void setFunction(Function<Object, ?> function) {
        this.function = function;
    }

    TypeFieldImpl copy(TypeImpl<?> newType) {
        return new TypeFieldImpl(this, newType);
    }

    @Override
    public Type<?> getDeclaringType() {
        return declaringType;
    }

    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(Object subject) {
        return (T) function.apply(subject);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", valueType='" + valueType + '\'' +
                ", function='" + function + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeFieldImpl typeField = (TypeFieldImpl) o;
        return name.equals(typeField.name) &&
                declaringType.equals(typeField.declaringType);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + declaringType.hashCode();
    }
}
