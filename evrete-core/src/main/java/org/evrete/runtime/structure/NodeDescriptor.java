package org.evrete.runtime.structure;

import org.evrete.api.Masked;
import org.evrete.util.Bits;
import org.evrete.util.CollectionUtils;
import org.evrete.util.NextIntSupplier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeDescriptor extends Node<NodeDescriptor> implements Masked {
    private final Bits mask = new Bits();
    private final NodeDescriptor[] sources;
    private final FactType[] factTypes;
    private FactType[][] evalGrouping = null;

    NodeDescriptor(NextIntSupplier idSupplier, Set<? extends NodeDescriptor> sources) {
        super(idSupplier, sources);
        this.sources = CollectionUtils.array(NodeDescriptor.class, sources.size());

        Set<FactType> types = new HashSet<>();
        for (NodeDescriptor source : sources) {
            types.addAll(Arrays.asList(source.factTypes));
            this.sources[source.getSourceIndex()] = source;
        }
        this.factTypes = FactType.toArray(types);

        for (FactType t : factTypes) {
            mask.set(t.getInRuleIndex());
        }
    }

    NodeDescriptor(NextIntSupplier idSupplier, FactType entryType) {
        super(idSupplier);
        this.sources = new NodeDescriptor[0];
        this.factTypes = new FactType[]{entryType};
        this.mask.set(entryType.getInRuleIndex());
    }

    private static boolean assertGrouping(FactType[] allTypes, FactType[][] grouping) {
        Set<FactType> all = new HashSet<>();
        for (FactType[] betaFactTypes : grouping) {
            all.addAll(Arrays.asList(betaFactTypes));
        }
        FactType[] allArr = FactType.toArray(all);
        FactType[] allCopy = FactType.toArray(Arrays.asList(allTypes));
        return Arrays.equals(allArr, allCopy);
    }

    public abstract boolean isConditionNode();

    @Override
    public Bits getMask() {
        return mask;
    }

    public FactType[] getTypes() {
        return factTypes;
    }

    public FactType[][] getEvalGrouping() {
        return evalGrouping;
    }

    void setEvalGrouping(FactType[][] grouping) {
        if (this.evalGrouping == null) {
            assert assertGrouping(this.factTypes, grouping);
            this.evalGrouping = grouping;
        } else {
            throw new IllegalStateException(this.toString());
        }
    }

    public final NodeDescriptor[] getSources() {
        return sources;
    }

}
