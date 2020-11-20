package org.evrete.runtime.aggregate;

import org.evrete.runtime.RuntimeAggregateLhsLoose;

class ExistsEvaluatorLoose extends AbstractEvaluatorLoose {

    public ExistsEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        super(aggregateLhs);
    }

    @Override
    public boolean getAsBoolean() {
        throw new UnsupportedOperationException();
/*
        for (ReIterator<ValueRow[]> it : keyReIterators) {
            if (it.reset() == 0) {
                return false;
            }
        }
        return alphaFactGroup == null || alphaFactGroup.getComputedFactCount() != 0;
*/
    }
}
