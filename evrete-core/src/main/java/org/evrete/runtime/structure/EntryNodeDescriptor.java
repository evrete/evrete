package org.evrete.runtime.structure;

import org.evrete.util.NextIntSupplier;

public class EntryNodeDescriptor extends NodeDescriptor {
    private final FactType factType;

    EntryNodeDescriptor(NextIntSupplier idSupplier, FactType factType) {
        super(idSupplier, factType);
        this.factType = factType;
    }

    public FactType getFactType() {
        return factType;
    }

    @Override
    public boolean isConditionNode() {
        return false;
    }

    @Override
    public String toString() {
        return "Entry{fact=" + factType +
                ", hash=" + hashCode() +
                '}';
    }
}
