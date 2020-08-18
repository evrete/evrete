package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.FactType;
import org.evrete.runtime.FactTypeField;
import org.evrete.runtime.builder.FieldReference;

import java.util.function.Function;

public class EvaluatorInternal implements ComplexityObject, LogicallyComparable {
    public static final EvaluatorInternal[] ZERO_ARRAY = new EvaluatorInternal[0];
    private final Evaluator delegate;
    private final FactTypeField[] descriptor;

    public EvaluatorInternal(Evaluator delegate, Function<NamedType, FactType> typeFunction) {
        this.delegate = delegate;
        this.descriptor = new FactTypeField[delegate.descriptor().length];
        for (int ref = 0; ref < delegate.descriptor().length; ref++) {
            FieldReference fieldReference = delegate.descriptor()[ref];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            this.descriptor[ref] = new FactTypeField(factType, field);
        }
    }

    public FactTypeField[] descriptor() {
        return descriptor;
    }

    public Evaluator getDelegate() {
        return delegate;
    }

    public boolean test(IntToValue values) {
        return delegate.test(values);
    }

    @Override
    public int compare(LogicallyComparable other) {
        return delegate.compare(other);
    }
}
