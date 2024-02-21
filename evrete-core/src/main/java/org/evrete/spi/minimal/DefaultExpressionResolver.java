package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;

class DefaultExpressionResolver implements ExpressionResolver {


    DefaultExpressionResolver() {
    }

    @Override
    @NonNull
    public FieldReference resolve(String arg, NamedType.Resolver resolver) {
        Type<?> type;
        TypeField field;
        NamedType typeRef;

        int firstDot = arg.indexOf('.');
        if (firstDot < 0) {
            // Var references type
            typeRef = resolver.resolve(arg);
            field = typeRef
                    .getType()
                    .getField(""); // empty value has a special meaning of "this" field
        } else {
            // Var references field
            String lhsFactType = arg.substring(0, firstDot);
            String dottedProp = arg.substring(firstDot + 1);
            Const.assertName(dottedProp);
            Const.assertName(lhsFactType.substring(1));

            typeRef = resolver.resolve(lhsFactType);
            type = typeRef.getType();
            field = type.getField(dottedProp);
        }
        return new FieldReferenceImpl(typeRef, field);
    }

}
