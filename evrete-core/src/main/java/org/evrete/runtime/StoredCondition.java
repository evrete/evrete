package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.events.ConditionEvaluationEvent;
import org.evrete.runtime.evaluation.ConditionEvaluationEventImpl;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.evrete.util.BroadcastingPublisher;
import org.evrete.util.Indexed;

import java.time.Instant;
import java.util.concurrent.Executor;

public class StoredCondition extends LhsCondition<ValuesPredicate, String, ActiveField> implements Indexed {
    private final DefaultEvaluatorHandle handle;
    private BroadcastingPublisher<ConditionEvaluationEvent> publisher;

    public StoredCondition(int index, ValuesPredicate predicate, double complexity, LhsField.Array<String, ActiveField> fields) {
        super(predicate, fields);
        this.handle = new DefaultEvaluatorHandle(index, complexity);
    }

    public DefaultEvaluatorHandle getHandle() {
        return handle;
    }

    public synchronized void setPredicate(ValuesPredicate predicate) {
        this.setCondition(predicate);
    }

    public synchronized Events.Publisher<ConditionEvaluationEvent> getCreatePublisher(Executor executor) {
        if(publisher == null) {
            publisher = new BroadcastingPublisher<>(executor);
        }
        return publisher;
    }

    public boolean test(RuleSession<?> context, IntToValue values) {
        if(publisher == null) {
            return getCondition().test(values);
        } else {
            Instant start = Instant.now();
            final boolean b = getCondition().test(values);
            Instant end = Instant.now();

            Object[] arguments = new Object[getDescriptor().length()];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = values.get(i);
            }
            publisher.broadcast(new ConditionEvaluationEventImpl(start, end, b, getCondition(), arguments));
            return b;
        }
    }


    @Override
    public int getIndex() {
        return handle.getIndex();
    }
}
