package org.evrete.api;

public interface EvaluationListener {

    void fire(Evaluator evaluator, IntToValue values, boolean result);


}
