package org.evrete.runtime;

public class EntryNodeDescriptor extends NodeDescriptor {
    private final FactType factType;

    EntryNodeDescriptor(FactType factType) {
        super(factType);
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
                '}';
    }
}
