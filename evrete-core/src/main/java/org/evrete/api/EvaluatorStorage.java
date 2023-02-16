package org.evrete.api;

public interface EvaluatorStorage {

    void addListener(EvaluationListener listener);

    void removeListener(EvaluationListener listener);

    EvaluatorHandle save(Evaluator evaluator, double complexity);
}
