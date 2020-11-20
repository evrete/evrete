package org.evrete.runtime.aggregate;

import org.evrete.runtime.RuntimeAggregateLhsLoose;

class NotExistsEvaluatorLoose extends AbstractEvaluatorLoose {

    public NotExistsEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        super(aggregateLhs);
    }

    @Override
    public boolean getAsBoolean() {
        throw new UnsupportedOperationException();
/*
        for (ReIterator<ValueRow[]> it : keyReIterators) {
            if (it.reset() == 0) {
                return true;
            }
        }

        if (alphaFactGroup == null) {
            return false;
        } else {
            return alphaFactGroup.getComputedFactCount() == 0;
        }
*/
    }
}
