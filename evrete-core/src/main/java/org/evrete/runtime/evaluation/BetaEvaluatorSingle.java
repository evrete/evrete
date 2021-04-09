package org.evrete.runtime.evaluation;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldReference;
import org.evrete.api.NamedType;
import org.evrete.api.TypeField;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class BetaEvaluatorSingle extends EvaluatorWrapper implements BetaEvaluator {
    public static final BetaEvaluatorSingle[] ZERO_ARRAY = new BetaEvaluatorSingle[0];
    private final BetaFieldReference[] descriptor;
    private final Bits factTypeMask;
    private final Set<FactType> descriptor1;
    private final Set<ActiveField> fields;
    private final EvaluatorWrapper[] constituents;

    BetaEvaluatorSingle(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        super(delegate);
        this.factTypeMask = new Bits();
        this.fields = new HashSet<>();
        this.constituents = new EvaluatorWrapper[]{this};
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

        this.descriptor1 = Collections.unmodifiableSet(factTypes);
    }

    private BetaEvaluatorSingle(BetaEvaluatorSingle other) {
        super(other);
        this.factTypeMask = other.factTypeMask;
        this.descriptor = other.descriptor;
        this.descriptor1 = other.descriptor1;
        this.fields = other.fields;
        this.constituents = new EvaluatorWrapper[]{this};
    }

    @Override
    public EvaluatorWrapper[] constituents() {
        return constituents;
    }

    @Override
    public boolean evaluatesField(ActiveField field) {
        return this.fields.contains(field);
    }

    @Override
    public Set<FactType> factTypes() {
        return descriptor1;
    }

    BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return getDelegate().toString();
    }

    @Override
    public Bits getFactTypeMask() {
        return factTypeMask;
    }

    @Override
    public BetaEvaluatorSingle copyOf() {
        return new BetaEvaluatorSingle(this);
    }
}
