package org.evrete.runtime;

import org.evrete.AbstractRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRuntimeRule extends AbstractRule {
    private final AbstractRuntime<?> runtime;
    private final Collection<FactType> factTypes;

    AbstractRuntimeRule(AbstractRuntime<?> runtime, AbstractRule other, Collection<FactType> factTypes) {
        super(other);
        this.runtime = runtime;
        this.factTypes = factTypes;
        setRhs(getLiteralRhs());
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
