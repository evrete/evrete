package org.evrete.runtime;

import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeDescriptor {
    private final Bits mask = new Bits();
    private final NodeDescriptor[] sources;
    private final FactType[] factTypes;

    NodeDescriptor(Set<? extends NodeDescriptor> sources) {
        this.sources = new NodeDescriptor[sources.size()];

        Set<FactType> types = new HashSet<>();
        int sourceId = 0;
        for (NodeDescriptor source : sources) {
            types.addAll(Arrays.asList(source.factTypes));
            this.sources[sourceId++] = source;
        }
        this.factTypes = FactType.toArray(types);

        for (FactType t : factTypes) {
            mask.set(t.getInRuleIndex());
        }
    }

    NodeDescriptor(FactType entryType) {
        this.sources = new NodeDescriptor[0];
        this.factTypes = new FactType[]{entryType};
        this.mask.set(entryType.getInRuleIndex());
    }

    public abstract boolean isConditionNode();

    Bits getFactTypeMask() {
        return mask;
    }

    public FactType[] getTypes() {
        return factTypes;
    }

    public final NodeDescriptor[] getSources() {
        return sources;
    }

}
