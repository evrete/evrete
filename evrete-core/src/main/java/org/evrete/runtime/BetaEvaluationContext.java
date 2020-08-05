package org.evrete.runtime;

public final class BetaEvaluationContext {
    private final FireContext fireContext;

    public BetaEvaluationContext(FireContext fireContext) {
        this.fireContext = fireContext;
    }

    public FireContext getFireContext() {
        return fireContext;
    }
}
