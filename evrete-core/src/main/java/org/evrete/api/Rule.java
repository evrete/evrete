package org.evrete.api;

import org.evrete.api.annotations.Nullable;

import java.util.function.Consumer;

public interface Rule extends FluentEnvironment<Rule>, Named, NamedType.Resolver {

    Consumer<RhsContext> getRhs();

    void setRhs(String literalRhs);

    void setRhs(@Nullable Consumer<RhsContext> rhs);

    int getSalience();

    void setSalience(int value);

    void chainRhs(Consumer<RhsContext> consumer);

    void setName(String newName);
}
