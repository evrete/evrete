package org.evrete.runtime;

import org.evrete.api.RuleDescriptor;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

public final class RuleDescriptorImpl extends AbstractRuntimeRule<FactType> implements RuleDescriptor {
    private final LhsDescriptor lhsDescriptor;

    private RuleDescriptorImpl(AbstractRuntime<?, ?> runtime, AbstractRule other, String ruleName, int salience, LhsDescriptor lhsDescriptor) {
        super(runtime, other, ruleName, salience, lhsDescriptor.getFactTypes());
        this.lhsDescriptor = lhsDescriptor;
    }
    static RuleDescriptorImpl factory(AbstractRuntime<?, ?> runtime, DefaultRuleBuilder<?> rule, LhsConditionHandles lhsConditions, int salience) {
        LhsDescriptor lhsDescriptor = new LhsDescriptor(runtime, rule.getLhs().getDeclaredFactTypes(), lhsConditions, new NextIntSupplier(), new MapFunction<>());
        return new RuleDescriptorImpl(runtime, rule, rule.getName(), salience, lhsDescriptor);
    }

    LhsDescriptor getLhs() {
        return lhsDescriptor;
    }

    @Override
    public RuleDescriptorImpl set(String property, Object value) {
        super.set(property, value);
        return this;
    }
}
