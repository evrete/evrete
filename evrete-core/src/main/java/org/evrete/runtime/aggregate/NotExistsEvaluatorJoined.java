package org.evrete.runtime.aggregate;

import org.evrete.api.KeysStore;
import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeLhs;

public class NotExistsEvaluatorJoined extends AbstractEvaluatorJoined {


    public NotExistsEvaluatorJoined(RuntimeLhs root, RuntimeAggregateLhsJoined aggregate) {
        super(root, aggregate);
    }

    @Override
    public boolean getAsBoolean() {
        KeysStore next = matchingValues();
        return next == null || next.isEmpty();
    }
}
