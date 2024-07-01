package org.evrete.api.events;

import org.evrete.api.annotations.NonNull;

import java.util.function.Consumer;

/**
 * Interface representing an {@link ContextEvent} that occurs when an environment property changes.
 */
public interface EnvironmentChangeEvent extends ContextEvent{
    /**
     * Gets the name of the property that changed.
     *
     * @return the name of the changed property
     */
    String getProperty();

    /**
     * Gets the value of the property after the change has occurred.
     *
     * @return the new value of the changed property
     */
    Object getValue();

    /**
     * Applies a given action if the specified property name matches and
     * the property value is assignable to the given type.
     *
     * @param property the name of the property to check.
     * @param type     the class type the property value should be assignable to.
     * @param action   the consumer action to be performed if conditions are satisfied.
     * @param <T>      the type parameter.
     */
    @SuppressWarnings("unchecked")
    default <T> void applyIf(@NonNull String property, Class<T> type, Consumer<T> action) {
        String prop = getProperty();
        Object value = getValue();
        if (prop.equals(property)) {
            if(value == null) {
                action.accept(null);
            } else if (type.isAssignableFrom(value.getClass())){
                action.accept((T) value);
            }
        }
    }
}
