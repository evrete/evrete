package org.evrete.runtime;

import org.evrete.AbstractRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRuntimeRule extends AbstractRule {
    //TODO !!! move to a new class when Stateless session is ready
    private final AbstractRuntime<?> runtime;
    private final Collection<FactType> factTypes;

    protected AbstractRuntimeRule(AbstractRuntime<?> runtime, AbstractRule other, Collection<FactType> factTypes) {
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
