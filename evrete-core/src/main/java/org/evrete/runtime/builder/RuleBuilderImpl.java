package org.evrete.runtime.builder;

import org.evrete.AbstractRule;
import org.evrete.api.FactBuilder;
import org.evrete.api.RhsContext;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.AbstractRuntime;

import java.util.function.Consumer;


public class RuleBuilderImpl<C extends RuntimeContext<C>> extends AbstractRule implements RuleBuilder<C> {
    private final AbstractRuntime<C> runtime;
    private final LhsBuilder<C> lhsBuilder;

    public RuleBuilderImpl(AbstractRuntime<C> ctx, String name) {
        super(name);
        if (ctx.ruleExists(name)) {
            throw new IllegalStateException("Rule '" + name + "' already exists");
        } else {
            this.runtime = ctx;
        }
        this.lhsBuilder = new LhsBuilder<>(this);
    }

    public RuleBuilderImpl<C> compileConditions(AbstractRuntime<?> runtime) {
        lhsBuilder.compileConditions(runtime);
        for (AggregateLhsBuilder<C> agg : lhsBuilder.getAggregateGroups()) {
            agg.compileConditions(runtime);
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C getRuntime() {
        return (C) runtime;
    }

    @Override
    public LhsBuilder<C> getLhs() {
        return lhsBuilder;
    }

    private C build() {
        switch (runtime.getKind()) {
            case SESSION:
                runtime.deployRule(runtime.compileRule(this));
                break;
            case KNOWLEDGE:
                runtime.compileRule(this);
                break;
            default:
                throw new IllegalStateException();
        }
        return getRuntime();
    }


    protected C build(Consumer<RhsContext> rhs) {
        setRhs(rhs);
        return build();
    }

    @Override
    public LhsBuilder<C> forEach(FactBuilder... facts) {
        return lhsBuilder.buildLhs(facts);
    }

    public AbstractRuntime<?> getRuntimeContext() {
        return runtime;
    }

}
