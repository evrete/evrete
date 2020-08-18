package org.evrete.runtime.aggregate;

import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.runtime.RuntimeAggregateLhsLoose;

class ExistsEvaluatorLoose extends AbstractEvaluatorLoose {

    public ExistsEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        super(aggregateLhs);
    }

    @Override
    public boolean getAsBoolean() {
        for (ReIterator<ValueRow[]> it : keyReIterators) {
            if (it.reset() == 0) {
                return false;
            }
        }
        return looseGroupIterator == null || looseGroupIterator.reset() != 0;
    }
}
