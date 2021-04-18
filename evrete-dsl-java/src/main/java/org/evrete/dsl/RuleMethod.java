package org.evrete.dsl;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class RuleMethod extends RuleSetMethod implements Consumer<RhsContext> {
    private final Rule ruleAnn;
    private final LhsParameter[] lhsParameters;
    private final Where predicates;
    private final int contextParamId;

    RuleMethod(RuleSetMethod method, Object instance) {
        super(method);
        this.ruleAnn = method.getAnnotation(Rule.class);
        this.predicates = method.getAnnotation(Where.class);
        Parameter[] parameters = method.getParameters();
        List<LhsParameter> lhsParameters = new ArrayList<>(parameters.length);
        int contextParamId = Integer.MIN_VALUE;
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (RhsContext.class.isAssignableFrom(param.getType())) {
                // Context Parameter
                if (contextParamId < 0) {
                    contextParamId = i;
                } else {
                    throw new MalformedResourceException("Duplicate context parameter in " + method.getMethodName());
                }
            } else {
                // LHS Parameter
                int invocationIndex = staticMethod ? i : i + 1;
                lhsParameters.add(new LhsParameter(param, invocationIndex));
            }
        }

        if (staticMethod) {
            this.contextParamId = contextParamId;
        } else {
            this.methodCurrentValues[0] = instance;
            this.contextParamId = contextParamId + 1;
        }
        this.lhsParameters = lhsParameters.toArray(LhsParameter.EMPTY_ARRAY);


    }

    Where getPredicates() {
        return predicates;
    }

    String getName() {
        String name = ruleAnn.value().trim();
        if (name.isEmpty()) {
            return method.getName();
        } else {
            return name;
        }
    }

    int getSalience() {
        return ruleAnn.salience();
    }

    LhsParameter[] getLhsParameters() {
        return lhsParameters;
    }

    @Override
    public void accept(RhsContext ctx) {
        for (LhsParameter p : lhsParameters) {
            this.methodCurrentValues[p.position] = ctx.getObject(p.getName());
        }
        if (contextParamId >= 0) {
            this.methodCurrentValues[contextParamId] = ctx;
        }
        try {
            handle.invokeWithArguments(methodCurrentValues);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
