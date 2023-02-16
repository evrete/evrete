package org.evrete.api;

public interface RuleBuilder<C extends RuntimeContext<C>> extends Rule, LhsFactSelector<LhsBuilder<C>> {

    LhsBuilder<C> getLhs();

    RuleBuilder<C> salience(int salience);

    <Z> RuleBuilder<C> property(String property, Z value);

    C getRuntime();

}
