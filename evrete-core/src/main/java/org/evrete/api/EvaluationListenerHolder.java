package org.evrete.api;

public interface EvaluationListenerHolder {
    void addListener(EvaluationListener listener);

    void removeListener(EvaluationListener listener);

}
