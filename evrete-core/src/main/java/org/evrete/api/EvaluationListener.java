package org.evrete.api;

import java.util.EventListener;

public interface EvaluationListener extends EventListener {

    void fire(Evaluator evaluator, IntToValue values, boolean result);

}
