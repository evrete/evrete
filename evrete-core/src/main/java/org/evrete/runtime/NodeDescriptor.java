package org.evrete.runtime;

import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeDescriptor {
    private final Bits factTypeMask = new Bits();
    private final Bits typeMask = new Bits();
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
            setMaskBits(t);
        }
    }

    NodeDescriptor(FactType entryType) {
        this.sources = new NodeDescriptor[0];
        this.factTypes = new FactType[]{entryType};
        setMaskBits(entryType);
    }

    private void setMaskBits(FactType t) {
        this.factTypeMask.set(t.getInRuleIndex());
        this.typeMask.set(t.getType().getId());
    }

    public abstract boolean isConditionNode();

    Bits getFactTypeMask() {
        return factTypeMask;
    }

    Bits getTypeMask() {
        return typeMask;
    }

    public FactType[] getTypes() {
        return factTypes;
    }

    public final NodeDescriptor[] getSources() {
        return sources;
    }

}
