package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationValues;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;

import java.util.function.Function;

public class BetaEvaluator extends EvaluatorWrapper implements Copyable<BetaEvaluator> {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final BetaFieldReference[] descriptor;
    private IntToValue stateValues;

    BetaEvaluator(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        super(delegate);
        FieldReference[] evaluatorDescriptor = delegate.descriptor();
        this.descriptor = new BetaFieldReference[evaluatorDescriptor.length];
        for (int i = 0; i < this.descriptor.length; i++) {
            FieldReference fieldReference = evaluatorDescriptor[i];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            this.descriptor[i] = new BetaFieldReference(factType, field);
        }
    }

    private BetaEvaluator(BetaEvaluator other) {
        super(other);
        this.descriptor = other.descriptor;
        this.stateValues = other.stateValues;
    }

    BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return getDelegate().toString();
    }

    @Override
    public BetaEvaluator copyOf() {
        return new BetaEvaluator(this);
    }

    void setEvaluationState(BetaEvaluationValues values) {
        this.stateValues = i -> values.apply(descriptor[i]);
    }

    public boolean test() {
        return test(this.stateValues);
    }
}
