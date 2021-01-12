package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationState;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;

import java.util.function.Function;

public class BetaEvaluator extends EvaluatorWrapper implements Copyable<BetaEvaluator> {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final BetaFieldReference[] descriptor;

    public BetaEvaluator(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        super(delegate);
        FieldReference[] evaluatorDescriptor = delegate.descriptor();
        this.descriptor = new BetaFieldReference[evaluatorDescriptor.length];
        for (int i = 0; i < this.descriptor.length; i++) {
            FieldReference fieldReference = evaluatorDescriptor[i];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            this.descriptor[i] = new BetaFieldReference(i, factType, field);
        }
    }

    private BetaEvaluator(BetaEvaluator other) {
        super(other);
        this.descriptor = other.descriptor;
    }

    public BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public BetaEvaluator copyOf() {
        return new BetaEvaluator(this);
    }

    public boolean test(BetaEvaluationState values) {
        return super.test(new IntToValue() {
            @Override
            public Object apply(int value) {
                BetaFieldReference ref = descriptor[value];
                return values.apply(ref.getFactType(), ref.getFieldIndex());
            }
        });
    }
}
