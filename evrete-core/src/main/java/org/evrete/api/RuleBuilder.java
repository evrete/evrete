package org.evrete.api;

public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, LhsFactSelector<LhsBuilder<C>> {

    LhsBuilder<C> getLhs();

    RuleBuilder<C> salience(int salience);

    @Override
    RuleBuilder<C> addImport(RuleScope scope, String imp);

    @Override
    RuleBuilder<C> addImport(RuleScope scope, Class<?> type);

    <Z> RuleBuilder<C> property(String property, Z value);

    @SuppressWarnings("unchecked")
    C getRuntime();
}
