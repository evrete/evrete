package org.evrete.runtime.evaluation;

import org.evrete.api.ActiveField;
import org.evrete.api.ComplexityObject;
import org.evrete.api.Copyable;
import org.evrete.api.EvaluationListeners;
import org.evrete.runtime.BetaEvaluationValues;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.Set;

public interface BetaEvaluator extends Copyable<BetaEvaluator>, EvaluationListeners, ComplexityObject {


    void setEvaluationState(BetaEvaluationValues values);

    boolean test();

    boolean evaluatesField(ActiveField field);

    Set<FactType> factTypes();

    default int getTotalTypesInvolved() {
        return factTypes().size();
    }

    Bits getFactTypeMask();

}
