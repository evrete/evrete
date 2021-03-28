package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.RuleScope;
import org.evrete.api.Type;
import org.evrete.util.Bits;

public abstract class AbstractRuntimeRule extends AbstractRule {
    final FactType[] factTypes;
    private final AbstractRuntime<?, ?> runtime;
    private final Bits typeMask = new Bits();

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, FactType[] factTypes) {
        this(runtime, other, other.getName(), other.getSalience(), factTypes);
    }

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, String ruleName, int salience, FactType[] factTypes) {
        super(other, ruleName, salience);
        this.runtime = runtime;
        this.factTypes = factTypes;
        for (FactType factType : factTypes) {
            this.typeMask.set(factType.getType().getId());
        }
        appendImports(runtime.getImportsData());
        setRhs(getLiteralRhs());
    }

    final boolean dependsOn(Type<?> type) {
        return typeMask.get(type.getId());
    }

    public FactType[] getFactTypes() {
        return factTypes;
    }

    @Override
    public final void setRhs(String literalRhs) {
        if (literalRhs != null) {
            setRhs(runtime.compile(literalRhs, factTypes, getImports(RuleScope.BOTH, RuleScope.RHS)));
        }
    }
}
