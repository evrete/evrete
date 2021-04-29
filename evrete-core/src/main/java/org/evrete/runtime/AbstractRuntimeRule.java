package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.NamedType;
import org.evrete.api.RuleScope;
import org.evrete.api.Type;
import org.evrete.util.NamedTypeImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public abstract class AbstractRuntimeRule<T extends FactType> extends AbstractRule {
    final T[] factTypes;
    private final AbstractRuntime<?, ?> runtime;
    private final Map<String, T> typeMapping = new HashMap<>();

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, T[] factTypes) {
        this(runtime, other, other.getName(), other.getSalience(), factTypes);
    }

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, String ruleName, int salience, T[] factTypes) {
        super(other, ruleName, salience);
        this.runtime = runtime;
        this.factTypes = factTypes;
        for (T factType : factTypes) {
            //this.typeMask.set(factType.type());
            if (typeMapping.put(factType.getName(), factType) != null) {
                throw new IllegalStateException();
            }
        }
        appendImports(runtime.getImports());
        setRhs(getLiteralRhs());
    }

    @Override
    public NamedType resolve(String var) {
        FactType factType = typeMapping.get(var);
        Type<?> t = runtime.getTypeResolver().getType(factType.type());
        return new NamedTypeImpl(t, factType.getName());
    }

    T resolveFactType(NamedType type) {
        return typeMapping.get(type.getName());
    }

    public T[] getFactTypes() {
        return factTypes;
    }

    @Override
    public final void setRhs(String literalRhs) {
        Collection<NamedType> namedTypes = new LinkedList<>();
        for (FactType factType : factTypes) {
            namedTypes.add(resolve(factType.getName()));
        }

        if (literalRhs != null) {
            setRhs(runtime.compile(literalRhs, namedTypes, getImports(), RuleScope.BOTH, RuleScope.RHS));
        }
    }
}
