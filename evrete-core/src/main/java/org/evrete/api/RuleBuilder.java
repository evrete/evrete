package org.evrete.api;

import org.evrete.runtime.builder.LhsBuilder;

public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, FactSelector<LhsBuilder<C>> {

    LhsBuilder<C> getLhs();

    C getRuntime();

}
