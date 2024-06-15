package org.evrete.runtime;

import org.evrete.api.LhsCondition;
import org.evrete.api.LhsField;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

/**
 * A subclass of the {@link LhsCondition} with the condition type set to {@link DefaultEvaluatorHandle}
 * @param <Fact>
 * @param <Field>
 */
public class LhsConditionDH<Fact, Field> extends LhsCondition<DefaultEvaluatorHandle, Fact, Field> {

    public LhsConditionDH(DefaultEvaluatorHandle condition, LhsField.Array<Fact, Field> descriptor) {
        super(condition, descriptor);
    }

    public LhsConditionDH(LhsConditionDH<?, ?> other, LhsField.Array<Fact, Field> descriptor) {
        super(other, descriptor);
    }
}
