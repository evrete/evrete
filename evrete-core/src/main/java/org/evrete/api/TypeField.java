package org.evrete.api;

/**
 * A representation of a Type field.
 */
public interface TypeField extends Named {

    /**
     * Returns the value type of the TypeField.
     *
     * @return the class representing the value type of the TypeField
     */
    Class<?> getValueType();


    /**
     * Returns the declaring {@link Type}
     * @return the declaring {@link Type}
     */
    Type<?> getDeclaringType();

    /**
     * Reads the value of a field from the given subject object.
     *
     * @param subject the Java object from which to read the field value
     * @param <T> the type of the field value
     * @return the field value
     */
    <T> T readValue(Object subject);
}
