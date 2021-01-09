package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiPredicate;

public class FactType implements Masked {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    public static final BiPredicate<FactType, FactType> EQUALITY_BY_INDEX = (t1, t2) -> t1.getInRuleIndex() == t2.getInRuleIndex();
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    private final String var;
    private final Type<?> type;
    private final AlphaBucketMeta alphaMask;
    private final FieldsKey fields;
    private final int inRuleIndex;
    private final Bits mask;
    private boolean uniqueKeyAndAlpha = true;
    private RhsFactGroupDescriptor factGroup;
    private int inGroupIndex = -1;

    public FactType(String var, Type<?> type, AlphaBucketMeta alphaMask, FieldsKey fields, int inRuleIndex) {
        this.var = var;
        this.type = type;
        this.alphaMask = alphaMask;
        this.fields = fields;
        this.inRuleIndex = inRuleIndex;
        this.mask = new Bits();
        this.mask.set(this.inRuleIndex);
    }

    FactType(FactType other) {
        assert other.factGroup != null;
        assert other.inGroupIndex >= 0;
        this.inGroupIndex = other.inGroupIndex;
        this.factGroup = other.factGroup;
        this.mask = other.mask;
        this.var = other.var;
        this.alphaMask = other.alphaMask;
        this.fields = other.fields;
        this.inRuleIndex = other.inRuleIndex;
        this.type = other.type;
        this.uniqueKeyAndAlpha = other.uniqueKeyAndAlpha;
    }

    public static FactType[] toArray(Collection<FactType> set) {
        FactType[] arr = set.toArray(FactType.ZERO_ARRAY);
        Arrays.sort(arr, COMPARATOR);
        return arr;
    }


    public RhsFactGroupDescriptor getFactGroup() {
        Objects.requireNonNull(factGroup);
        return factGroup;
    }

    void setFactGroup(RhsFactGroupDescriptor factGroup) {
        if (this.factGroup == null) {
            this.factGroup = factGroup;
            this.inGroupIndex = factGroup.positionOf(this);
        } else {
            throw new IllegalStateException();
        }
    }

    public int getBucketIndex() {
        return alphaMask.getBucketIndex();
    }

    public void markNonUniqueKeyAndAlpha() {
        this.uniqueKeyAndAlpha = false;
    }

    public boolean isUniqueKeyAndAlpha() {
        return uniqueKeyAndAlpha;
    }

    public int findFieldPosition(TypeField field) {
        ActiveField[] arr = fields.getFields();
        for (int i = 0; i < arr.length; i++) {
            ActiveField f = arr[i];
            if (f.getDelegate() == field) return i;
        }
        throw new IllegalStateException();
    }

    public FieldsKey getFields() {
        return fields;
    }

    public AlphaBucketMeta getAlphaMask() {
        return alphaMask;
    }

    public String getVar() {
        return var;
    }

    public Type<?> getType() {
        return fields.getType();
    }

    public int getInRuleIndex() {
        return inRuleIndex;
    }

    public int getInGroupIndex() {
        return inGroupIndex;
    }

    @Override
    public Bits getMask() {
        return mask;
    }

    @Override
    public String toString() {
        return "FactType{" +
                "var='" + var + '\'' +
                ", type=" + type +
                //", fields=" + fields +
                '}';
    }
}
