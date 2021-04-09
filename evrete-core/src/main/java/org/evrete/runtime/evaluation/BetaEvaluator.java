package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.Set;

public interface BetaEvaluator extends Copyable<BetaEvaluator>, EvaluationListeners, ComplexityObject {

    boolean test();

    boolean evaluatesField(ActiveField field);

    Set<FactType> factTypes();

    default int getTotalTypesInvolved() {
        return factTypes().size();
    }

    Bits getFactTypeMask();

    EvaluatorWrapper[] constituents();

    @Override
    default void addListener(EvaluationListener listener) {
        for (EvaluatorWrapper e : constituents()) {
            e.addListener(listener);
        }
    }

    @Override
    default void removeListener(EvaluationListener listener) {
        for (EvaluatorWrapper e : constituents()) {
            e.removeListener(listener);
        }
    }

}
