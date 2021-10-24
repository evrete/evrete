package org.evrete.api;

public interface FluentEnvironment<X> extends Environment {
    X set(String property, Object value);
}
