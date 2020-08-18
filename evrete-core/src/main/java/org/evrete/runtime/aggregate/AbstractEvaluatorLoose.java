package org.evrete.runtime.aggregate;

import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.runtime.RhsFactGroupIterator;
import org.evrete.runtime.RhsKeysGroupIterator;
import org.evrete.runtime.RuntimeAggregateLhsLoose;

import java.util.function.BooleanSupplier;

abstract class AbstractEvaluatorLoose implements BooleanSupplier {
    final RhsFactGroupIterator looseGroupIterator;
    final ReIterator<ValueRow[]>[] keyReIterators;

    @SuppressWarnings("unchecked")
    AbstractEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        this.looseGroupIterator = aggregateLhs.getLooseFactGroupIterator();

        RhsKeysGroupIterator[] keyGroupIterators = aggregateLhs.getKeyGroupIterators();
        this.keyReIterators = new ReIterator[keyGroupIterators.length];
        for (int i = 0; i < keyGroupIterators.length; i++) {
            this.keyReIterators[i] = keyGroupIterators[i].getMainIterator();
        }

    }
}
