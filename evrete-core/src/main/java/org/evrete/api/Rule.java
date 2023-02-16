package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends FluentEnvironment<Rule>, Named, NamedType.Resolver {

    Consumer<RhsContext> getRhs();

    void setRhs(String literalRhs);

    void setRhs(Consumer<RhsContext> rhs);

    int getSalience();

    void setSalience(int value);

    void chainRhs(Consumer<RhsContext> consumer);

    void setName(String newName);
}
