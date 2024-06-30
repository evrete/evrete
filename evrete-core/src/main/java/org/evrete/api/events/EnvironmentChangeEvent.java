package org.evrete.api.events;

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
}
