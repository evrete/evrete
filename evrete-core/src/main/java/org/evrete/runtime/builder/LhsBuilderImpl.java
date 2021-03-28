package org.evrete.runtime.builder;

import org.evrete.api.RhsContext;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;

import java.util.function.Consumer;

public class LhsBuilderImpl<C extends RuntimeContext<C>> extends AbstractLhsBuilder<C, LhsBuilderImpl<C>> {

    LhsBuilderImpl(RuleBuilderImpl<C> ruleBuilder) {
        super(ruleBuilder);
    }

    @Override
    protected LhsBuilderImpl<C> self() {
        return this;
    }

    @Override
    public C execute(Consumer<RhsContext> consumer) {
        return getRuleBuilder().build(consumer);
    }

    @Override
    public C execute(String literalRhs) {
        return getRuleBuilder().build(literalRhs);
    }

    @Override
    public RuleBuilder<C> setRhs(String literalConsumer) {
        RuleBuilderImpl<C> builder = getRuleBuilder();
        builder.setRhs(literalConsumer);
        return builder;
    }

    @Override
    public RuleBuilder<C> create() {
        return getRuleBuilder();
    }

    @Override
    public C execute() {
        return getRuleBuilder().build();
    }
}
