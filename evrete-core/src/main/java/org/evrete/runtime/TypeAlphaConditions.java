package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.Indexed;

import java.util.Set;

/**
 * <p>
 * A indexed subset of the alpha conditions of an active type.
 * </p>
 * <p>
 * Each subset is assigned an index that serves as a unique identifier Rete's alpha memory bucket.
 * In a rule set, several fact declarations may use the same set of alpha conditions and, consequently, may reuse
 * the same alpha memory.
 * </p>
 */
public class TypeAlphaConditions implements Indexed {
    private final Mask<AlphaConditionHandle> mask;
    private final int index;
    private final ActiveType.Idx type;

    public TypeAlphaConditions(int index, ActiveType.Idx type, Set<AlphaConditionHandle> alphaConditions) {
        this.index = index;
        this.type = type;
        this.mask = Mask.alphaConditionsMask().set(alphaConditions);
    }

    public ActiveType.Idx getType() {
        return type;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public Mask<AlphaConditionHandle> getMask() {
        return mask;
    }


    @Override
    public String toString() {
        if(mask.cardinality() == 0) {
            return "{" +
                    "type=" + type.getIndex() +
                    '}';
        } else {
            return "{" +
                    "type=" + type.getIndex() +
                    ", mask=" + mask +
                    '}';
        }
    }
}


