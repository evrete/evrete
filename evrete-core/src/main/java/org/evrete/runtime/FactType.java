package org.evrete.runtime;

import org.evrete.api.Masked;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class FactType implements Masked {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    private final String var;
    private final AlphaBucketMeta alphaMask;
    private final FieldsKey fields;
    private final int inRuleIndex;
    private final Bits mask;
    private boolean uniqueKeyAndAlpha = true;

    public FactType(String var, AlphaBucketMeta alphaMask, FieldsKey fields, int inRuleIndex) {
        this.var = var;
        this.alphaMask = alphaMask;
        this.fields = fields;
        this.inRuleIndex = inRuleIndex;
        this.mask = new Bits();
        this.mask.set(this.inRuleIndex);
    }

    FactType(FactType other) {
        this.mask = other.mask;
        this.var = other.var;
        this.alphaMask = other.alphaMask;
        this.fields = other.fields;
        this.inRuleIndex = other.inRuleIndex;
        this.uniqueKeyAndAlpha = other.uniqueKeyAndAlpha;
    }

    public static FactType[] toArray(Collection<FactType> set) {
        FactType[] arr = set.toArray(FactType.ZERO_ARRAY);
        Arrays.sort(arr, COMPARATOR);
        return arr;
    }

    int getBucketIndex() {
        return alphaMask.getBucketIndex();
    }

    void markNonUniqueKeyAndAlpha() {
        this.uniqueKeyAndAlpha = false;
    }

    boolean isUniqueKeyAndAlpha() {
        return uniqueKeyAndAlpha;
    }

    int findFieldPosition(TypeField field) {
        ActiveField[] arr = fields.getFields();
        for (int i = 0; i < arr.length; i++) {
            ActiveField f = arr[i];
            if (f.getDelegate().equals(field)) return i;
        }
        throw new IllegalStateException("Field not found");
    }

    public FieldsKey getFields() {
        return fields;
    }

    AlphaBucketMeta getAlphaMask() {
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

    @Override
    public Bits getMask() {
        return mask;
    }
}
