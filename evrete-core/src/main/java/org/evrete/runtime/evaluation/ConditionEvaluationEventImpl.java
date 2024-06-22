package org.evrete.runtime.evaluation;

import org.evrete.api.RuleSession;
import org.evrete.api.ValuesPredicate;
import org.evrete.api.events.ConditionEvaluationEvent;
import org.evrete.runtime.AbstractRuleSession;
import org.evrete.runtime.events.AbstractTimedEvent;

import java.time.Instant;

public class ConditionEvaluationEventImpl extends AbstractTimedEvent implements ConditionEvaluationEvent {
    private final boolean passed;
    private final ValuesPredicate predicate;
    private final Object[] arguments;
    private final RuleSession<?> context;

    public ConditionEvaluationEventImpl(RuleSession<?> context, Instant startTime, Instant endTime, boolean passed, ValuesPredicate predicate, Object[] arguments) {
        super(startTime, endTime);
        this.passed = passed;
        this.predicate = predicate;
        this.arguments = arguments;
        this.context = context;
    }

    @Override
    public ValuesPredicate getCondition() {
        return predicate;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public boolean isPassed() {
        return passed;
    }

    @Override
    public RuleSession<?> getContext() {
        return context;
    }
}
