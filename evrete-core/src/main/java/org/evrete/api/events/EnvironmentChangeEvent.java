package org.evrete.api.events;

//TODO doc
public interface EnvironmentChangeEvent extends ContextEvent{
    String getProperty();

    Object getValue();
}
