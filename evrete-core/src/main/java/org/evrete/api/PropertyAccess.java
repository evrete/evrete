package org.evrete.api;

import java.util.Collection;

public interface PropertyAccess {
    <T> void set(String property, T value);

    <T> T get(String property);

    default <T> T get(String name, T defaultValue) {
        T obj = get(name);
        return obj == null ? defaultValue : obj;
    }

    Collection<String> getPropertyNames();

}
