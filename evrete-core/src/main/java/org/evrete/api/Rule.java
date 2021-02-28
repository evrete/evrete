package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends PropertyAccess<Rule>, Named, FluentImports<Rule> {
    Consumer<RhsContext> getRhs();

    void setRhs(String literalRhs);

    void setRhs(Consumer<RhsContext> rhs);

    int getSalience();

    void setSalience(int value);

    //TODO add to tests
    void chainRhs(Consumer<RhsContext> consumer);
}
