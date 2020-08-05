package org.evrete.runtime.structure;

import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.builder.AggregateLhsBuilder;
import org.evrete.runtime.builder.RootLhsBuilder;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RootLhsDescriptor extends LhsDescriptor {
    private final Set<AggregateLhsDescriptor> aggregateDescriptors = new HashSet<>();
    private final MapFunction<NamedType, FactType> rootMapping;
    private final FactType[] allFactTypes;


    public RootLhsDescriptor(AbstractRuntime<?, ?> runtime, RootLhsBuilder<?> root, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> rootMapping) {
        super(runtime, null, root, factIdGenerator, rootMapping);
        Collection<FactType> allFacts = new HashSet<>(getGroupFactTypes());
        this.rootMapping = rootMapping;

        for (AggregateLhsBuilder<?> aggregateBuilder : root.getAggregateGroups()) {
            AggregateLhsDescriptor aggregateDescriptor = new AggregateLhsDescriptor(runtime, this, aggregateBuilder, factIdGenerator, new MapFunction<>());
            allFacts.addAll(aggregateDescriptor.getGroupFactTypes());
            this.aggregateDescriptors.add(aggregateDescriptor);
        }


        this.allFactTypes = new FactType[allFacts.size()];
        for (FactType factType : allFacts) {
            this.allFactTypes[factType.getInRuleIndex()] = factType;
        }

    }

    public FactType[] getAllFactTypes() {
        return allFactTypes;
    }

    public Set<AggregateLhsDescriptor> getAggregateDescriptors() {
        return aggregateDescriptors;
    }

    MapFunction<NamedType, FactType> getRootMapping() {
        return rootMapping;
    }
}


