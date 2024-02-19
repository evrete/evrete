package org.evrete.api;

/**
 * A representation of a Type field. Implementations are expected
 * to implement equals() and hashCode() methods.
 */
public interface TypeField extends Named {

    /**
     * Returns the type on which this TypeField is declared.
     *
     * @return the declaring type of the TypeField
     */
    Type<?> getDeclaringType();

    /**
     * Returns the value type of the TypeField.
     *
     * @return the class representing the value type of the TypeField
     */
    Class<?> getValueType();

    /**
     * Reads the value of a field from the given subject object.
     *
     * @param subject the Java object from which to read the field value
     * @param <T> the type of the field value
     * @return the field value
     */
    <T> T readValue(Object subject);
}
