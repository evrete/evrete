package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.Type;
import org.evrete.util.Bits;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRuntimeRule extends AbstractRule {
    private final AbstractRuntime<?> runtime;
    protected final FactType[] factTypes;

    private final Bits typeMask = new Bits();

    AbstractRuntimeRule(AbstractRuntime<?> runtime, AbstractRule other, FactType[] factTypes) {
        super(other);
        this.runtime = runtime;
        this.factTypes = factTypes;
        for (FactType factType : factTypes) {
            this.typeMask.set(factType.getType().getId());
        }
        setRhs(getLiteralRhs());
    }

    final boolean dependsOn(Type<?> type) {
        return typeMask.get(type.getId());
    }

    @Override
    public final void setRhs(String literalRhs) {
        if (literalRhs != null) {
            Set<String> allImports = new HashSet<>(runtime.getImports());
            allImports.addAll(getImports());
            setRhs(runtime.compile(literalRhs, factTypes, allImports));
        }
    }
}
