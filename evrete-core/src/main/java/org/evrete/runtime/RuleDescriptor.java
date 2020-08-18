package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

public class RuleDescriptor extends AbstractRule {
    private final LhsDescriptor lhsDescriptor;

    RuleDescriptor(AbstractRuntime<?> runtime, RuleBuilderImpl<?> rule) {
        super(rule);
        RuleBuilderImpl<?> compiled = rule.compileConditions(runtime);
        this.lhsDescriptor = new LhsDescriptor(runtime, compiled.getLhs(), new NextIntSupplier(), new MapFunction<>());
    }

    public LhsDescriptor getLhs() {
        return lhsDescriptor;
    }

}
