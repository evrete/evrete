package org.evrete.runtime.evaluation;

import org.evrete.api.ComplexityObject;
import org.evrete.api.EvaluatorHandle;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.util.Mask;

import java.util.Set;

public interface BetaEvaluator extends ComplexityObject {

    boolean evaluatesField(ActiveField field);

    Set<FactType> factTypes();

    default int getTotalTypesInvolved() {
        return factTypes().size();
    }

    Mask<FactType> getFactTypeMask();

    EvaluatorHandle[] constituents();
}
