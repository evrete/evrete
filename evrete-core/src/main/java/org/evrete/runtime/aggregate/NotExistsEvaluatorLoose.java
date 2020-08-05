package org.evrete.runtime.aggregate;

import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.runtime.RuntimeAggregateLhsLoose;

public class NotExistsEvaluatorLoose extends AbstractEvaluatorLoose {

    public NotExistsEvaluatorLoose(RuntimeAggregateLhsLoose aggregateLhs) {
        super(aggregateLhs);
    }

    @Override
    public boolean getAsBoolean() {
        for (ReIterator<ValueRow[]> it : keyReIterators) {
            if (it.reset() == 0) {
                return true;
            }
        }

        if (looseGroupIterator == null) {
            return false;
        } else {
            return looseGroupIterator.reset() == 0;
        }
    }
}
