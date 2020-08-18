package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends Named {
    Consumer<RhsContext> getRhs();

    <T> void setProperty(String name, T value);

    <T> T getProperty(String name);

    <T> T getProperty(String name, T defaultValue);

    Rule setRhs(Consumer<RhsContext> rhs);
}
