package org.evrete.runtime.evaluation;

import org.evrete.api.NamedType;
import org.evrete.api.TypeField;
import org.evrete.runtime.FactType;
import org.evrete.runtime.FactTypeField;
import org.evrete.runtime.builder.FieldReference;

import java.util.function.Function;

public class BetaEvaluator extends EvaluatorWrapper {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final FactTypeField[] descriptor;

    public BetaEvaluator(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        super(delegate);
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

}
