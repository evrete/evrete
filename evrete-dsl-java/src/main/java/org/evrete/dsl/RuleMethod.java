package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Where;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class RuleMethod extends MethodWithValues implements Consumer<RhsContext> {
    private final LhsParameter[] lhsParameters;
    private final int contextParamId;
    private final int salience;
    private final String ruleName;
    private final Where predicates;

    RuleMethod(MethodHandles.Lookup lookup, Method method, TypeResolver typeResolver) {
        super(lookup, method);
        this.salience = Utils.salience(method);
        this.ruleName = Utils.ruleName(method);
        this.predicates = method.getAnnotation(Where.class);
        Parameter[] parameters = method.getParameters();
        List<LhsParameter> lhsParameters = new ArrayList<>(parameters.length);
        int contextParamId = Integer.MIN_VALUE;
        if (!method.getReturnType().equals(void.class)) {
            throw new MalformedResourceException("Rule methods must be void. " + method);
        }
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (RhsContext.class.isAssignableFrom(param.getType())) {
                // Context Parameter
                if (contextParamId < 0) {
                    contextParamId = i;
                } else {
                    throw new MalformedResourceException("Duplicate context parameter in " + method.getName());
                }
            } else {
                // LHS Parameter
                int invocationIndex = staticMethod ? i : i + 1;
                lhsParameters.add(new LhsParameter(typeResolver, param, invocationIndex));
            }
        }

        if (staticMethod) {
            this.contextParamId = contextParamId;
        } else {
            this.contextParamId = contextParamId + 1;
        }
        this.lhsParameters = lhsParameters.toArray(LhsParameter.EMPTY_ARRAY);
    }


    String getRuleName() {
        return ruleName;
    }

    int getSalience() {
        return salience;
    }

    LhsParameter[] getLhsParameters() {
        return lhsParameters;
    }

    Where getPredicates() {
        return predicates;
    }

    @Override
    public void accept(RhsContext ctx) {
        for (LhsParameter p : lhsParameters) {
            this.methodCurrentValues[p.position] = ctx.getObject(p.getName());
        }
        if (contextParamId >= 0) {
            this.methodCurrentValues[contextParamId] = ctx;
        }
        call();
    }

    private static class LhsParameter extends FactBuilder implements NamedType {
        static LhsParameter[] EMPTY_ARRAY = new LhsParameter[0];
        final int position;
        private final Type<?> type;

        LhsParameter(TypeResolver typeResolver, Parameter parameter, int position) {
            super(factName(parameter), Utils.box(parameter.getType()));
            this.position = position;
            this.type = typeResolver.getOrDeclare(getResolvedType());
        }

        private static String factName(Parameter parameter) {
            Fact fact = parameter.getAnnotation(Fact.class);
            if (fact != null) {
                return fact.value();
            } else {
                return parameter.getName();
            }
        }

        @Override
        public Type<?> getType() {
            return type;
        }

        @Override
        public String getVar() {
            return getName();
        }
    }
}
