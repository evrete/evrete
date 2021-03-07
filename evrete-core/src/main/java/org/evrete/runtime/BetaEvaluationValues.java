package org.evrete.runtime;

@FunctionalInterface
public interface BetaEvaluationValues {

    Object apply(BetaFieldReference ref);
}
