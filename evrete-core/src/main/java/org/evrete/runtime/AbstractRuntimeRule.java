package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.NamedType;
import org.evrete.api.RuleScope;
import org.evrete.api.Type;
import org.evrete.util.Bits;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractRuntimeRule extends AbstractRule {
    final FactType[] factTypes;
    private final AbstractRuntime<?, ?> runtime;
    private final Bits typeMask = new Bits();
    private final Map<String, FactType> typeMapping = new HashMap<>();

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, FactType[] factTypes) {
        this(runtime, other, other.getName(), other.getSalience(), factTypes);
    }

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, String ruleName, int salience, FactType[] factTypes) {
        super(other, ruleName, salience);
        this.runtime = runtime;
        this.factTypes = factTypes;
        for (FactType factType : factTypes) {
            this.typeMask.set(factType.getType().getId());
            if (typeMapping.put(factType.getVar(), factType) != null) {
                throw new IllegalStateException();
            }
        }
        appendImports(runtime.getImportsData());
        setRhs(getLiteralRhs());
    }

    @Override
    public FactType resolve(String var) {
        return Objects.requireNonNull(typeMapping.get(var), "No such type: '" + var + "'");
    }

    FactType resolve(NamedType type) {
        return resolve(type.getVar());
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
            setRhs(runtime.compile(literalRhs, factTypes, getImportsData(), RuleScope.BOTH, RuleScope.RHS));
        }
    }
}
