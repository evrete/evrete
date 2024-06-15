package org.evrete.runtime;

public class GroupedFactType extends FactType {
    private final int inGroupIndex;

    public GroupedFactType(FactType other, int inGroupIndex) {
        super(other);
        this.inGroupIndex = inGroupIndex;
    }

    protected GroupedFactType(GroupedFactType other) {
        super(other);
        this.inGroupIndex = other.inGroupIndex;
    }

    public int getInGroupIndex() {
        return inGroupIndex;
    }
}
