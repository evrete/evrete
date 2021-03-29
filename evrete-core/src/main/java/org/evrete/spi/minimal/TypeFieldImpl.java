package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.function.Function;

class TypeFieldImpl implements TypeField {
    private final String name;
    private final Function<Object, ?> function;
    private final Class<?> valueType;
    private final TypeImpl<?> declaringType;
    private final int id;

    TypeFieldImpl(int id, TypeImpl<?> declaringType, String name, Class<?> valueType, Function<Object, ?> function) {
        this.id = id;
        this.name = name;
        this.valueType = valueType;
        this.function = function;
        this.declaringType = declaringType;
    }

    @Override
    public int getId() {
        return id;
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
                ", declaringType='" + declaringType.getName() + '\'' +
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
