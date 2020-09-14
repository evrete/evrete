package org.evrete.api;

import java.util.Collection;

public interface PropertyAccess {
    <T> void setProperty(String property, T value);

    <T> T getProperty(String property);

    default <T> T getProperty(String name, T defaultValue) {
        T obj = getProperty(name);
        return obj == null ? defaultValue : obj;
    }

    Collection<String> getPropertyNames();

}
