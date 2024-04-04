package org.evrete.api;

import java.util.Collection;

/**
 * The Environment interface provides a way to store and retrieve property values.
 * It allows setting and getting properties with a specified name and value.
 * Additionally, it provides a method to retrieve all property names in the environment.
 */
public interface Environment {

    /**
     * Sets a property in the environment with the specified name and value.
     *
     * @param property The name of the property to be set.
     * @param value The value of the property to be set.
     * @return The previous value of the property, or {@code null} if it did not have one.
     */
    Object set(String property, Object value);

    /**
     * Retrieves the value of a property by its name.
     *
     * @param <T> The expected type of the property value.
     * @param property The name of the property to retrieve.
     * @return The value of the property as type T, or {@code null} if it does not exist.
     */
    <T> T get(String property);

    /**
     * Retrieves the value of a property by its name. If the property does not exist,
     * it returns a default value specified by the caller.
     *
     * @param <T> The expected type of the property value.
     * @param name The name of the property to retrieve.
     * @param defaultValue The default value to return if the property does not exist.
     * @return The value of the property as type T, or the defaultValue if the property does not exist.
     */
    default <T> T get(String name, T defaultValue) {
        T obj = get(name);
        return obj == null ? defaultValue : obj;
    }

    /**
     * Retrieves a collection of all property names in the environment.
     *
     * @return A collection of strings representing all property names in the environment.
     */
    Collection<String> getPropertyNames();

}
