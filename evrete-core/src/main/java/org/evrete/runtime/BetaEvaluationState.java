package org.evrete.runtime;

@FunctionalInterface
public interface BetaEvaluationState {

    Object apply(FactType factType, int fieldIndex);
}
