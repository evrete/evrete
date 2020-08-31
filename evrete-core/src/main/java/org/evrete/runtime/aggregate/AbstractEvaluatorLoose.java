package org.evrete.runtime.aggregate;

import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.runtime.*;

import java.util.function.BooleanSupplier;

abstract class AbstractEvaluatorLoose implements BooleanSupplier {
    final RhsFactGroupAlpha alphaFactGroup;
    final ReIterator<ValueRow[]>[] keyReIterators;

    AbstractEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        throw new UnsupportedOperationException();
/*
        this.alphaFactGroup = aggregateLhs.getAlphaFactGroup();

        RhsFactGroupBeta[] betaFactGroups = aggregateLhs.getBetaFactGroups();
        this.keyReIterators = new ReIterator[betaFactGroups.length];
        for (int i = 0; i < betaFactGroups.length; i++) {
            this.keyReIterators[i] = keyGroupIterators[i].getMainIterator();
        }

*/
    }
}
