package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends Named, FluentImports<Rule> {
    Consumer<RhsContext> getRhs();

    int getSalience();

    void setSalience(int value);

    <T> void setProperty(String property, T value);

    <T> T getProperty(String property);

    default <T> T getProperty(String name, T defaultValue) {
        T obj = getProperty(name);
        return obj == null ? defaultValue : obj;
    }

    void setRhs(String literalRhs);

    void setRhs(Consumer<RhsContext> rhs);

    void chainRhs(Consumer<RhsContext> consumer);
}
