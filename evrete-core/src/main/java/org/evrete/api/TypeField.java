package org.evrete.api;

/**
 * A representation of a Type field. Implementations are expected
 * to implement equals() and hashCode() methods as well.
 */
public interface TypeField extends Named {
    TypeField[] ZERO_ARRAY = new TypeField[0];

    int getId();

    Type<?> getDeclaringType();

    Class<?> getValueType();

    Object readValue(Object subject);

}
