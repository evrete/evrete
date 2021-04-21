package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class BetaEvaluatorSingle implements BetaEvaluator {
    public static final BetaEvaluatorSingle[] ZERO_ARRAY = new BetaEvaluatorSingle[0];
    private final BetaFieldReference[] descriptor;
    private final Bits factTypeMask;
    private final Set<FactType> factTypes;
    private final Set<ActiveField> fields;
    private final EvaluatorHandle[] constituents;
    private final EvaluatorHandle delegate;

    BetaEvaluatorSingle(EvaluatorHandle delegate, Function<NamedType, FactType> typeFunction) {
        this.delegate = delegate;
        this.factTypeMask = new Bits();
        this.fields = new HashSet<>();
        this.constituents = new EvaluatorHandle[]{delegate};
        FieldReference[] evaluatorDescriptor = delegate.descriptor();
        this.descriptor = new BetaFieldReference[evaluatorDescriptor.length];
        Set<FactType> factTypes = new HashSet<>();
        for (int i = 0; i < this.descriptor.length; i++) {
            FieldReference fieldReference = evaluatorDescriptor[i];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            BetaFieldReference bfr = new BetaFieldReference(factType, field);
            this.descriptor[i] = bfr;
            factTypeMask.set(factType.getInRuleIndex());
            fields.add(bfr.getActiveField());
            factTypes.add(factType);
        }

        this.factTypes = Collections.unmodifiableSet(factTypes);
    }

    @Override
    public EvaluatorHandle[] constituents() {
        return constituents;
    }

    @Override
    public double getComplexity() {
        return delegate.getComplexity();
    }

    @Override
    public boolean evaluatesField(ActiveField field) {
        return this.fields.contains(field);
    }

    @Override
    public Set<FactType> factTypes() {
        return factTypes;
    }

    BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Bits getFactTypeMask() {
        return factTypeMask;
    }
}
