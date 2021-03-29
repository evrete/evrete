package org.evrete.api;

/**
 * A representation of a Type field. Implementations are expected
 * to implement equals() and hashCode() methods as well.
 */
public interface TypeField extends Named {

    int getId();

    Type<?> getDeclaringType();

    Class<?> getValueType();

    <T> T readValue(Object subject);
}
