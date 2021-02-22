package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

public class RuleDescriptor extends AbstractRuntimeRule {
    private final LhsDescriptor lhsDescriptor;

    private RuleDescriptor(AbstractRuntime<?> runtime, AbstractRule other, LhsDescriptor lhsDescriptor) {
        super(runtime, other, lhsDescriptor.getFactTypes());
        this.lhsDescriptor = lhsDescriptor;
    }

    static RuleDescriptor factory(AbstractRuntime<?> runtime, RuleBuilderImpl<?> rule) {
        RuleBuilderImpl<?> compiled = rule.compileConditions(runtime);
        LhsDescriptor lhsDescriptor = new LhsDescriptor(runtime, compiled.getLhs(), new NextIntSupplier(), new MapFunction<>());
        return new RuleDescriptor(runtime, rule, lhsDescriptor);
    }

    public LhsDescriptor getLhs() {
        return lhsDescriptor;
    }

    @Override
    public RuleDescriptor set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @Override
    public RuleDescriptor addImport(String imp) {
        super.addImport(imp);
        return this;
    }


/*
    @Override
    public void addListener(EvaluationListener listener) {
        for (RhsFactGroupDescriptor d : lhsDescriptor.getAllFactGroups()) {
            ConditionNodeDescriptor finalNode = d.getFinalNode();
            if (finalNode != null) {
                finalNode.forEachConditionNode(node -> node.addListener(listener));
            }
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (RhsFactGroupDescriptor d : lhsDescriptor.getAllFactGroups()) {
            ConditionNodeDescriptor finalNode = d.getFinalNode();
            if (finalNode != null) {
                finalNode.forEachConditionNode(node -> node.removeListener(listener));
            }
        }
    }
*/
}
