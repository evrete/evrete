package org.evrete.api;

/**
 * The {@code FluentEnvironment} interface extends the functionality of {@link Environment} to
 * support fluent API style configuration by allowing properties to be set in a chainable manner.
 *
 * @param <X> the type of the environment implementation that extends this interface, used to enable
 *            method chaining by returning the same type upon setting a property.
 */
public interface FluentEnvironment<X> extends Environment {

    /**
     * Sets the specified property to the given value.
     *
     * @param property the name of the property to set; should not be {@code null}.
     * @param value the new value for the property; the actual type is determined by the
     *        implementation and the property being set.
     * @return the current instance of {@code X} to allow for method chaining.
     */
    X set(String property, Object value);
}
