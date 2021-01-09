package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.FactType;
import org.evrete.runtime.FactTypeField;
import org.evrete.runtime.builder.FieldReference;

import java.util.function.Function;

public class BetaEvaluator implements ComplexityObject, LogicallyComparable {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final EvaluatorWrapper delegate;
    private final FactTypeField[] descriptor;

    public BetaEvaluator(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        this.delegate = delegate;
        this.descriptor = new FactTypeField[delegate.descriptor().length];
        for (int ref = 0; ref < delegate.descriptor().length; ref++) {
            FieldReference fieldReference = delegate.descriptor()[ref];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            this.descriptor[ref] = new FactTypeField(factType, field);
        }
    }

    public FactTypeField[] betaDescriptor() {
        return descriptor;
    }

    public boolean test(IntToValue values) {
        return delegate.test(values);
    }

    @Override
    public int compare(LogicallyComparable other) {
        return delegate.compare(other);
    }

    @Override
    public double getComplexity() {
        return delegate.getComplexity();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
