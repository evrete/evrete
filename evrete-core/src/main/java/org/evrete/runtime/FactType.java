package org.evrete.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * Runtime representation of an LHS fact declaration.
 */
public class FactType extends AbstractLhsFact {
    public static final FactType[] EMPTY_ARRAY = new FactType[0];
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

    public static <T extends FactType> String toSimpleDebugString(T[] types) {
        return toSimpleDebugString(Arrays.asList(types));
    }

    public static <T extends FactType> String toSimpleDebugString(Collection<T> types) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (FactType type : types) {
            joiner.add("'" + type.getVarName() + "'");
        }
        return joiner.toString();
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
