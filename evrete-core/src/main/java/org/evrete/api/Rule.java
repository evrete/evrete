package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends PropertyAccess, Named, FluentImports<Rule> {
    Consumer<RhsContext> getRhs();

    int getSalience();

    void setSalience(int value);

    void setRhs(String literalRhs);

    void setRhs(Consumer<RhsContext> rhs);

    void chainRhs(Consumer<RhsContext> consumer);
}
