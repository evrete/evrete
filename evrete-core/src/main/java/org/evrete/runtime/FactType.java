package org.evrete.runtime;

import org.evrete.api.TypeField;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class FactType {
    public static final FactType[] ZERO_ARRAY = new FactType[0];
    private static final Comparator<FactType> COMPARATOR = Comparator.comparingInt(FactType::getInRuleIndex);
    private final String name;
    private final MemoryAddress memoryAddress;
    private final int inRuleIndex;
    private final Mask<MemoryAddress> memoryMask;

    public FactType(String name, MemoryAddress memoryAddress, int inRuleIndex) {
        this.name = name;
        this.memoryAddress = memoryAddress;
        this.inRuleIndex = inRuleIndex;
        this.memoryMask = Mask.addressMask();
        this.memoryMask.set(memoryAddress);
    }

    public FactType(FactType other) {
        this.name = other.name;
        this.memoryAddress = other.memoryAddress;
        this.inRuleIndex = other.inRuleIndex;
        this.memoryMask = other.memoryMask;
    }

    public static FactType[] toArray(Collection<FactType> set) {
        FactType[] arr = set.toArray(FactType.ZERO_ARRAY);
        Arrays.sort(arr, COMPARATOR);
        return arr;
    }

    public Mask<MemoryAddress> getMemoryMask() {
        return memoryMask;
    }

    public int findFieldPosition(TypeField field) {
        ActiveField[] arr = memoryAddress.fields().getFields();
        for (int i = 0; i < arr.length; i++) {
            ActiveField f = arr[i];
            if (f.getName().equals(field.getName())) return i;
        }
        throw new IllegalStateException("Field not found: " + field);
    }


    public MemoryAddress getMemoryAddress() {
        return memoryAddress;
    }

    public String getName() {
        return name;
    }

    public int type() {
        return memoryAddress.fields().type();
    }

    public int getInRuleIndex() {
        return inRuleIndex;
    }

    @Override
    public String toString() {
        return name;
    }
}
