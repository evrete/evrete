package org.evrete.spi.minimal;

import org.evrete.api.TypeField;

import java.util.function.Function;

class TypeFieldImpl implements TypeField {
    private final String name;
    private final Class<?> valueType;
    private final Function<Object, ?> function;
    private final TypeImpl<?> declaringType;

    TypeFieldImpl(String name, TypeImpl<?> declaringType, Class<?> valueType, Function<Object, ?> function) {
        this.name = name;
        this.valueType = valueType;
        this.function = function;
        this.declaringType = declaringType;
    }

    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    public TypeImpl<?> getDeclaringType() {
        return declaringType;
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
                ", function=" + function +
                '}';
    }
}
