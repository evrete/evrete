package org.evrete.runtime.builder;

import org.evrete.AbstractRule;
import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;

import java.util.Collection;
import java.util.function.Consumer;

public class RuleBuilderImpl<C extends RuntimeContext<C>> extends AbstractRule implements RuleBuilder<C> {
    public static final int NULL_SALIENCE = Integer.MIN_VALUE;
    private final AbstractRuntime<?, C> runtime;
    private final LhsBuilderImpl<C> lhsBuilder;

    public RuleBuilderImpl(AbstractRuntime<?, C> ctx, String name) {
        super(name, NULL_SALIENCE);
        this.runtime = ctx;
        this.appendImports(ctx.getImports());
        this.lhsBuilder = new LhsBuilderImpl<>(this);
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
    public NamedType resolve(String var) {
        return lhsBuilder.resolve(var);
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
        this.set(property, value);
        return this;
    }


    @Override
    public LhsBuilderImpl<C> getLhs() {
        return lhsBuilder;
    }

    C build() {
        runtime.compileRule(this);
        return getRuntime();
    }

    @SuppressWarnings("unchecked")
    @Override
    public C getRuntime() {
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
    public LhsBuilderImpl<C> forEach(Collection<FactBuilder> facts) {
        return lhsBuilder.buildLhs(facts);
    }

    public AbstractRuntime<?, C> getRuntimeContext() {
        return runtime;
    }

}
