package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.annotations.NonNull;
import org.evrete.util.NamedTypeImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public abstract class AbstractRuntimeRule<T extends FactType> extends AbstractRule {
    final T[] factTypes;
    private final AbstractRuntime<?, ?> runtime;
    private final Map<String, T> typeMapping = new HashMap<>();
    private final DefaultTypeResolver typeResolver = new DefaultTypeResolver();


    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, T[] factTypes) {
        this(runtime, other, other.getName(), other.getSalience(), factTypes);
    }

    AbstractRuntimeRule(AbstractRuntime<?, ?> runtime, AbstractRule other, String ruleName, int salience, T[] factTypes) {
        super(other, ruleName, salience);
        this.runtime = runtime;
        this.factTypes = factTypes;
        for (T factType : factTypes) {
            Type<?> t = runtime.getTypeResolver().getType(factType.type());
            NamedType namedType = new NamedTypeImpl(t, factType.getName());
            this.typeResolver.save(namedType);
            //this.typeMask.set(factType.type());
            if (typeMapping.put(factType.getName(), factType) != null) {
                throw new IllegalStateException();
            }
        }
        setRhs(getLiteralRhs());
    }

    @Override
    @NonNull
    public NamedType resolve(@NonNull String var) {
        return typeResolver.resolve(var);
    }

    @Override
    public Collection<NamedType> getDeclaredFactTypes() {
        return typeResolver.getDeclaredFactTypes();
    }

    T resolveFactType(NamedType type) {
        return typeMapping.get(type.getName());
    }

    public T[] getFactTypes() {
        return factTypes;
    }

    @Override
    public final void setRhs(String literalRhs) {
        if (literalRhs != null) {
            Collection<NamedType> namedTypes = new LinkedList<>();
            for (FactType factType : factTypes) {
                namedTypes.add(resolve(factType.getName()));
            }
            setRhs(runtime.compileRHS(literalRhs, namedTypes));
        }
    }
}
