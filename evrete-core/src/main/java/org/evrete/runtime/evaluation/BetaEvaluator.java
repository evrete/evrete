package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationState;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;

import java.util.function.Function;

public class BetaEvaluator extends EvaluatorWrapper implements Copyable<BetaEvaluator> {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final BetaFieldReference[] descriptor;
    private IntToValue stateValues;
    private BetaEvaluationState values;

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
        this.values = other.values;
    }

    BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public BetaEvaluator copyOf() {
        return new BetaEvaluator(this);
    }

    void setEvaluationState(final ValueResolver resolver, BetaEvaluationState values) {
        final Argument[] arguments = new Argument[descriptor.length];
        for (int i = 0; i < arguments.length; i++) {
            BetaFieldReference ref = descriptor[i];
            arguments[i] = new Argument(values, ref.getFactType(), ref.getFieldIndex());
        }

        this.stateValues = i -> resolver.getValue(arguments[i].getValue());
    }

    public boolean test() {
        return test(this.stateValues);
    }

    private static class Argument {
        private final BetaEvaluationState values;
        private final FactType factType;
        private final int index;

        Argument(BetaEvaluationState values, FactType factType, int index) {
            this.values = values;
            this.factType = factType;
            this.index = index;
        }

        ValueHandle getValue() {
            return values.apply(factType, index);
        }
    }
}
