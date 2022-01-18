package org.evrete.runtime;

public class RuleActivationResult {
    final long executions;
    final FactActionBuffer actionBuffer;

    public RuleActivationResult(long executions, FactActionBuffer actionBuffer) {
        this.executions = executions;
        this.actionBuffer = actionBuffer;
    }
}
