package org.evrete.dsl;

import org.evrete.api.FactBuilder;
import org.evrete.dsl.annotation.Fact;

import java.lang.reflect.Parameter;

class LhsParameter extends FactBuilder {
    static LhsParameter[] EMPTY_ARRAY = new LhsParameter[0];
    final int position;

    LhsParameter(Parameter parameter, int position) {
        super(factName(parameter), Utils.box(parameter.getType()));
        this.position = position;
    }

    private static String factName(Parameter parameter) {
        Fact fact = parameter.getAnnotation(Fact.class);
        if (fact != null) {
            return fact.value();
        } else {
            return parameter.getName();
        }
    }
}
