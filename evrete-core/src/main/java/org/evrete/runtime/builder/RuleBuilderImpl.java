package org.evrete.runtime.builder;

import org.evrete.AbstractRule;
import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;

import java.util.Collection;
import java.util.function.Consumer;


//TODO !!!! why extending AbstractRule?
public class RuleBuilderImpl<C extends RuntimeContext<C>> extends AbstractRule implements RuleBuilder<C> {
    private final AbstractRuntime<?, C> runtime;
    private final LhsBuilder<C> lhsBuilder;

    public RuleBuilderImpl(AbstractRuntime<?, C> ctx, String name, int defaultSalience) {
        super(name, defaultSalience);
/*
        if (ctx.ruleExists(name)) {
            throw new IllegalStateException("Rule '" + name + "' already exists");
        } else {
            this.runtime = ctx;
        }
*/
        this.runtime = ctx;
        this.appendImports(ctx.getImportsData());
        this.lhsBuilder = new LhsBuilder<>(this);
    }

    public RuleBuilderImpl<C> compileConditions(AbstractRuntime<?, ?> runtime) {
        lhsBuilder.compileConditions(runtime);
        return this;
    }

    @Override
    public RuleBuilderImpl<C> set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    public RuleBuilderImpl<C> salience(int salience) {
        setSalience(salience);
        return this;
    }

    @Override
    public RuleBuilderImpl<C> addImport(RuleScope scope, String imp) {
        super.addImport(scope, imp);
        return this;
    }

    @Override
    public RuleBuilder<C> addImport(RuleScope scope, Class<?> type) {
        super.addImport(scope, type);
        return this;
    }

    @Override
    public <Z> RuleBuilder<C> property(String property, Z value) {
        set(property, value);
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

    @SuppressWarnings("unchecked")
    C build() {
        runtime.compileRule(this);
        return (C) runtime;
    }


    C build(Consumer<RhsContext> rhs) {
        setRhs(rhs);
        return build();
    }

    C build(String literalRhs) {
        setRhs(literalRhs);
        return build();
    }

    @Override
    public LhsBuilder<C> forEach(Collection<FactBuilder> facts) {
        return lhsBuilder.buildLhs(facts);
    }

    public AbstractRuntime<?, C> getRuntimeContext() {
        return runtime;
    }

}
