package org.evrete.runtime.builder;

import org.evrete.AbstractRule;
import org.evrete.api.FactBuilder;
import org.evrete.api.RhsContext;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.AbstractRuntime;

import java.util.function.Consumer;

public class RuleBuilderImpl<C extends RuntimeContext<C, ?>> extends AbstractRule implements RuleBuilder<C> {
    private final AbstractRuntime<C, ?> runtime;
    private final RootLhsBuilder<C> outputGroup;

    public RuleBuilderImpl(AbstractRuntime<C, ?> ctx, String name) {
        super(name);
        //this.rhs = new RuleRhs(name);
        if (ctx.ruleExists(name)) {
            throw new IllegalStateException("Rule '" + name + "' already exists");
        } else {
            this.runtime = ctx;
        }

        this.outputGroup = new RootLhsBuilder<>(this);
    }

    public RuleBuilderImpl<C> compileConditions(AbstractRuntime<?, ?> runtime) {
        outputGroup.compileConditions(runtime);
        for (AggregateLhsBuilder<C> agg : outputGroup.getAggregateGroups()) {
            agg.compileConditions(runtime);
        }
        return this;
    }

    @Override
    public RootLhsBuilder<C> getOutputGroup() {
        return outputGroup;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C deploy() {
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
        return (C) runtime;

    }

    @Override
    public C execute(Consumer<RhsContext> consumer) {
        this.initRhs(consumer);
        return deploy();
    }

    @Override
    public RootLhsBuilder<C> forEach(FactBuilder... facts) {
        return outputGroup.buildLhs(facts);
    }

    public AbstractRuntime<?, ?> getRuntimeContext() {
        return runtime;
    }

}
