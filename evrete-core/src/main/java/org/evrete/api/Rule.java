package org.evrete.api;

import java.util.function.Consumer;

public interface Rule extends Named {
    Consumer<RhsContext> getRhs();

    void setRhs(Consumer<RhsContext> rhs);
}
