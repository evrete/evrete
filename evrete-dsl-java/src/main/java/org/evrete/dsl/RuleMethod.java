package org.evrete.dsl;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Where;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class RuleMethod extends ClassMethod implements SessionCloneable<RuleMethod>, Consumer<RhsContext> {
    private final int contextParamId;
    private final int salience;
    private final String ruleName;
    final String[] stringPredicates;
    final MethodPredicate[] methodPredicates;
    final FactDeclaration[] factDeclarations;

    RuleMethod(MethodHandles.Lookup lookup, Method method) {
        super(lookup, method);
        this.salience = Utils.salience(method);
        this.ruleName = Utils.ruleName(method);
        Where predicates = method.getAnnotation(Where.class);
        if (predicates == null) {
            this.stringPredicates = new String[0];
            this.methodPredicates = new MethodPredicate[0];
        } else {
            this.stringPredicates = predicates.value();
            this.methodPredicates = predicates.asMethods();
        }

        int ctxIndex = Integer.MIN_VALUE;
        if (!method.getReturnType().equals(void.class)) {
            throw new MalformedResourceException("Rule methods must be void. " + method);
        }
        Parameter[] parameters = method.getParameters();
        List<FactDeclaration> rhsParameterList = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (RhsContext.class.isAssignableFrom(param.getType())) {
                // Context Parameter
                if (ctxIndex < 0) {
                    ctxIndex = i;
                } else {
                    throw new MalformedResourceException("Duplicate context parameter in " + method.getName());
                }
            } else {
                FactDeclaration rhsParameter = new FactDeclaration(param, i);
                rhsParameterList.add(rhsParameter);
            }
        }
        this.factDeclarations = rhsParameterList.toArray(FactDeclaration.EMPTY);
        this.contextParamId = ctxIndex;
    }


    private RuleMethod(RuleMethod other, Object instance) {
        super(other, instance);
        this.contextParamId = other.contextParamId;
        this.salience = other.salience;
        this.ruleName = other.ruleName;
        this.stringPredicates = other.stringPredicates;
        this.methodPredicates = other.methodPredicates;
        this.factDeclarations = other.factDeclarations;
    }

    @Override
    public RuleMethod copy(Object sessionInstance) {
        return new RuleMethod(this, sessionInstance);
    }

    String getRuleName() {
        return ruleName;
    }

    int getSalience() {
        return salience;
    }

    @Override
    public void accept(RhsContext ctx) {
        for (FactDeclaration p : factDeclarations) {
            this.args[p.position] = ctx.getObject(p.name);
        }
        if (contextParamId >= 0) {
            this.args[contextParamId] = ctx;
        }
        call();
    }

    static class FactDeclaration {
        static final FactDeclaration[] EMPTY = new FactDeclaration[0];
        final int position;
        final String name;
        final Class<?> type;

        FactDeclaration(Parameter parameter, int position) {
            this.name = Utils.factName(parameter);
            this.position = position;
            this.type = parameter.getType();
        }
    }
}
