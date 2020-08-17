package org.evrete.runtime.structure;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.AlphaBucketMeta;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.util.Bits;
import org.evrete.util.NextIntSupplier;

import java.util.*;
import java.util.function.BiPredicate;

public class FactType implements Masked {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    public static final BiPredicate<FactType, FactType> EQUALITY_BY_INDEX = (t1, t2) -> t1.getInRuleIndex() == t2.getInRuleIndex();
    private final String var;
    private final Type type;
    private final AlphaBucketMeta alphaMask;
    private final FieldsKey fields;
    private final int inRuleIndex;
    private final Bits mask;
    private boolean uniqueKeyAndAlpha = true;
    private RhsFactGroupDescriptor factGroup;
    private int inGroupIndex = -1;

    private FactType(AbstractRuntime<?> runtime, FactTypeBuilder builder, Set<Evaluator> alphaConditions, NextIntSupplier factIdGenerator) {
        this.type = builder.getType();
        this.var = builder.getVar();
        this.inRuleIndex = factIdGenerator.next();

        Collection<ActiveField> activeFields = new HashSet<>();
        for (TypeField f : builder.getBetaTypeFields()) {
            activeFields.add(runtime.getCreateActiveField(f));
        }

        this.fields = new FieldsKey(type, activeFields);
        this.mask = new Bits();
        this.mask.set(this.inRuleIndex);
        this.alphaMask = runtime.getCreateAlphaMask(fields, builder.isBetaTypeBuilder(), alphaConditions);
    }

    protected FactType(FactType other) {
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

    static FactType factory(AbstractRuntime<?> runtime, FactTypeBuilder builder, Set<Evaluator> alphaConditions, NextIntSupplier factIdGenerator) {
        return new FactType(
                runtime,
                builder,
                alphaConditions,
                factIdGenerator
        );
    }

    public RhsFactGroupDescriptor getFactGroup() {
        Objects.requireNonNull(factGroup);
        return factGroup;
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

    void setFactGroup(RhsFactGroupDescriptor factGroup) {
        if (this.factGroup == null) {
            this.factGroup = factGroup;
            this.inGroupIndex = factGroup.positionOf(this);
        } else {
            throw new IllegalStateException();
        }
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

    public Type getType() {
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
