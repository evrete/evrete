package org.evrete.runtime.aggregate;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ValueRow;
import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeLhs;
import org.evrete.runtime.structure.FactType;

import java.util.function.BooleanSupplier;

abstract class AbstractEvaluatorJoined implements BooleanSupplier {
    private final IntToValueRow parentValues;
    private final KeysStore aggregateData;

    AbstractEvaluatorJoined(RuntimeLhs root, RuntimeAggregateLhsJoined aggregate) {
        this.aggregateData = aggregate.getSuccessData();

        ValueRow[][] rootKeyState = root.getKeyState();
        FactType[] rootFactTypes = aggregate.getDescriptor().getJoinCondition().getLevelData()[0].getFactTypeSequence();
        long[] mapping = new long[rootFactTypes.length];

        for (int i = 0; i < mapping.length; i++) {
            FactType type = rootFactTypes[i];
            int keyIndex = type.getFactGroup().getKeyGroupIndex();
            int factIndex = type.getInGroupIndex();
            mapping[i] = (((long) keyIndex) << 32) + factIndex;
        }

        this.parentValues = i -> {
            long addr = mapping[i];
            return rootKeyState[(int) (addr >> 32)][(int) addr];
        };
    }

    KeysStore matchingValues() {
        return aggregateData.getNext(parentValues);
    }
}
