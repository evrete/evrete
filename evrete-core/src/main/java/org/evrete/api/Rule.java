package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends Named {
    Consumer<RhsContext> getRhs();

    int getSalience();

    void setSalience(int value);

    <T> void setProperty(String property, T value);

    <T> T getProperty(String property);

    default <T> T getProperty(String name, T defaultValue) {
        T obj = getProperty(name);
        return obj == null ? defaultValue : obj;
    }

    Rule setRhs(Consumer<RhsContext> rhs);

    Rule chainRhs(Consumer<RhsContext> consumer);
}
