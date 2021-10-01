package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends FluentEnvironment<Rule>, Named, FluentImports<Rule>, NamedType.Resolver {

    void setRhs(String literalRhs);

    void setRhs(Consumer<RhsContext> rhs);

    Consumer<RhsContext> getRhs();

    int getSalience();

    void setSalience(int value);

    void chainRhs(Consumer<RhsContext> consumer);
}
