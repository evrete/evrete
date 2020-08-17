package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.BetaConditionNode;

import java.util.ArrayList;
import java.util.Collection;

public class RuntimeListeners implements Copyable<RuntimeListeners>, Listeners {
    private final Collection<EvaluationListener> evaluationListeners = new ArrayList<>();

    @Override
    public RuntimeListeners copyOf() {
        RuntimeListeners newInstance = new RuntimeListeners();
        newInstance.evaluationListeners.addAll(evaluationListeners);
        return newInstance;
    }

    @Override
    public void addConditionTestListener(EvaluationListener listener) {
        this.evaluationListeners.add(listener);
    }


    public boolean containsConditionTestListener() {
        return evaluationListeners.size() > 0;
    }

    public void fireConditionTestResult(BetaConditionNode node, Evaluator evaluator, IntToValue values, boolean result) {
        for (EvaluationListener listener : evaluationListeners) {
            listener.apply(node, evaluator, values, result);
        }
    }
}
