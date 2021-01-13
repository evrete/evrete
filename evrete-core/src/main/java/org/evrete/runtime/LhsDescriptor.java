package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.LhsBuilder;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

import java.util.Collection;
import java.util.HashSet;

public class LhsDescriptor extends AbstractLhsDescriptor {
    private final FactType[] allFactTypes;

    public LhsDescriptor(AbstractRuntime<?> runtime, LhsBuilder<?> root, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> rootMapping) {
        super(runtime, null, root, factIdGenerator, rootMapping);
        Collection<FactType> allFacts = new HashSet<>(getGroupFactTypes());
        this.allFactTypes = new FactType[allFacts.size()];
        for (FactType factType : allFacts) {
            this.allFactTypes[factType.getInRuleIndex()] = factType;
        }
    }

    public FactType[] getAllFactTypes() {
        return allFactTypes;
    }
}


