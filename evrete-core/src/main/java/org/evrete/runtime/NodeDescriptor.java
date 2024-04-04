package org.evrete.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeDescriptor {
    private final Mask<FactType> factTypeMask = Mask.factTypeMask();
    private final Mask<MemoryAddress> memoryMask = Mask.addressMask();
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
        this.factTypeMask.set(t);
        this.memoryMask.set(t.getMemoryAddress());
    }

    public abstract boolean isConditionNode();

    Mask<FactType> getFactTypeMask() {
        return factTypeMask;
    }

    public Mask<MemoryAddress> getMemoryMask() {
        return memoryMask;
    }


    public FactType[] getTypes() {
        return factTypes;
    }

    public final NodeDescriptor[] getSources() {
        return sources;
    }

}
