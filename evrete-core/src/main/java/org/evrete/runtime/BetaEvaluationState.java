package org.evrete.runtime;

import org.evrete.api.ValueHandle;

@FunctionalInterface
public interface BetaEvaluationState {

    ValueHandle apply1(FactType factType, int fieldIndex);
}
