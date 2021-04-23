package org.evrete.runtime;

import org.evrete.api.TypeField;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class FactType {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    private final String name;
    private final AlphaBucketMeta alphaMask;
    private final FieldsKey fields;
    private final int inRuleIndex;

    FactType(String name, AlphaBucketMeta alphaMask, FieldsKey fields, int inRuleIndex) {
        this.name = name;
        this.alphaMask = alphaMask;
        this.fields = fields;
        this.inRuleIndex = inRuleIndex;
    }

    FactType(FactType other) {
        this.name = other.name;
        this.alphaMask = other.alphaMask;
        this.fields = other.fields;
        this.inRuleIndex = other.inRuleIndex;
    }

    public static FactType[] toArray(Collection<FactType> set) {
        FactType[] arr = set.toArray(FactType.ZERO_ARRAY);
        Arrays.sort(arr, COMPARATOR);
        return arr;
    }


    int findFieldPosition(TypeField field) {
        ActiveField[] arr = fields.getFields();
        for (int i = 0; i < arr.length; i++) {
            ActiveField f = arr[i];
            if (f.field() == field.getId()) return i;
        }
        throw new IllegalStateException("Field not found");
    }

    public FieldsKey getFields() {
        return fields;
    }

    AlphaBucketMeta getAlphaMask() {
        return alphaMask;
    }

    public String getName() {
        return name;
    }


    public int type() {
        return fields.type();
    }

    public int getInRuleIndex() {
        return inRuleIndex;
    }

    @Override
    public String toString() {
        return name;
    }
}
