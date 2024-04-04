package org.evrete.runtime.evaluation;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.WorkUnit;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.runtime.Mask;

import java.util.Set;

public interface BetaEvaluator extends WorkUnit {

    boolean evaluatesField(ActiveField field);

    Set<FactType> factTypes();

    default int getTotalTypesInvolved() {
        return factTypes().size();
    }

    Mask<FactType> getFactTypeMask();

    EvaluatorHandle[] constituents();
}
