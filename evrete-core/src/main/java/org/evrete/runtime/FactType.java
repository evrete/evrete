package org.evrete.runtime;

/**
 * Runtime representation of an LHS fact declaration.
 */
public class FactType extends AbstractLhsFact {
    public static FactType[] EMPTY_ARRAY = new FactType[0];
    private final ActiveType activeType;
    private final AlphaAddress alphaAddress;

    FactType(AbstractLhsFact other, ActiveType activeType, AlphaAddress alphaAddress) {
        super(other);
        this.activeType = activeType;
        this.alphaAddress = alphaAddress;
    }

    // TODO review usage and delete
    public ActiveType.Idx typeId() {
        return activeType.getId();
    }

    public ActiveType type() {
        return activeType;
    }

    public FactType(FactType other) {
        super(other);
        this.activeType = other.activeType;
        this.alphaAddress = other.alphaAddress;
    }

    public AlphaAddress getAlphaAddress() {
        return alphaAddress;
    }

    @Override
    public String toString() {
        return "{" +
                "var='" + getVarName() + "'" +
                ", type=" + activeType.getId() +
                ", address=" + alphaAddress +
                '}';
    }
}
