package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class FactType implements NamedType {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    private final String var;
    private final AlphaBucketMeta alphaMask;
    private final FieldsKey fields;
    private final int inRuleIndex;

    FactType(String var, AlphaBucketMeta alphaMask, FieldsKey fields, int inRuleIndex) {
        this.var = var;
        this.alphaMask = alphaMask;
        this.fields = fields;
        this.inRuleIndex = inRuleIndex;
    }

    public static FactType[] toArray(Collection<FactType> set) {
        FactType[] arr = set.toArray(FactType.ZERO_ARRAY);
        Arrays.sort(arr, COMPARATOR);
        return arr;
    }


    int findFieldPosition(TypeField field) {
        ActiveField[] arr = fields.getFields();
        for (int i = 0; i < arr.length; i++) {
            ActiveFieldImpl f = (ActiveFieldImpl) arr[i];
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

    @Override
    public String getVar() {
        return var;
    }

    @Override
    public Type<?> getType() {
        return fields.getType();
    }

    public int getInRuleIndex() {
        return inRuleIndex;
    }

    @Override
    public String toString() {
        return "{" +
                "var='" + var + '\'' +
                ", type='" + fields.getType().getName() +
                "'}";
    }
}
