package org.evrete.runtime.structure;

import org.evrete.AbstractRule;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

public class RuleDescriptor extends AbstractRule {
    private final RootLhsDescriptor rootLhsDescriptor;

    public RuleDescriptor(AbstractRuntime<?> runtime, RuleBuilderImpl<?> rule) {
        super(rule);
        RuleBuilderImpl<?> compiled = rule.compileConditions(runtime);
        this.rootLhsDescriptor = new RootLhsDescriptor(runtime, compiled.getLhs(), new NextIntSupplier(), new MapFunction<>());
    }

    public RootLhsDescriptor getRootLhsDescriptor() {
        return rootLhsDescriptor;
    }

}
