package org.evrete.api;

import org.evrete.runtime.builder.RootLhsBuilder;

import java.util.function.Consumer;

public interface RuleBuilder<C extends RuntimeContext<C, ?>> extends Named, FactSelector<RootLhsBuilder<C>> {

    RootLhsBuilder<C> getOutputGroup();

    C deploy();

    C execute(Consumer<RhsContext> consumer);
}
