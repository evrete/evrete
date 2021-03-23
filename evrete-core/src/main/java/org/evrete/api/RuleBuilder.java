package org.evrete.api;

import org.evrete.runtime.builder.LhsBuilder;

public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, LhsFactSelector<LhsBuilder<C>> {

    LhsBuilder<C> getLhs();

    C getRuntime();

    RuleBuilder<C> salience(int salience);

    @Override
    RuleBuilder<C> addImport(RuleScope scope, String imp);

    @Override
    RuleBuilder<C> addImport(RuleScope scope, Class<?> type);

    <Z> RuleBuilder<C> property(String property, Z value);

}
