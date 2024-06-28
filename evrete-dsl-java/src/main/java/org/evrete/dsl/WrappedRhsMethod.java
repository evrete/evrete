package org.evrete.dsl;

import org.evrete.api.RhsContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class WrappedRhsMethod extends WrappedCloneableMethod<WrappedRhsMethod> implements Consumer<RhsContext> {
    final RuleMethod.FactDeclaration[] factDeclarations;
    private final int contextParamId;

    public WrappedRhsMethod(WrappedClass declaringClass, Method delegate) {
        super(declaringClass, delegate);

        int ctxIndex = Integer.MIN_VALUE;
        if (!delegate.getReturnType().equals(void.class)) {
            throw new MalformedResourceException("Rule methods must be void: " + delegate);
        }

        List<RuleMethod.FactDeclaration> rhsParameterList = new ArrayList<>();
        Parameter[] parameters = delegate.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (RhsContext.class.isAssignableFrom(param.getType())) {
                // Context Parameter
                if (ctxIndex < 0) {
                    ctxIndex = i;
                } else {
                    throw new MalformedResourceException("Duplicate context parameter in " + delegate.getName());
                }
            } else {
                RuleMethod.FactDeclaration rhsParameter = new RuleMethod.FactDeclaration(param, i);
                rhsParameterList.add(rhsParameter);
            }
        }
        this.factDeclarations = rhsParameterList.toArray(RuleMethod.FactDeclaration.EMPTY);
        this.contextParamId = ctxIndex;
    }

    public WrappedRhsMethod(WrappedRhsMethod other, Object bindInstance) {
        super(other, bindInstance);
        this.factDeclarations = other.factDeclarations;
        this.contextParamId = other.contextParamId;
    }

    @Override
    WrappedRhsMethod bindTo(Object classInstance) {
        return new WrappedRhsMethod(this, classInstance);
    }

    @Override
    public void accept(RhsContext ctx) {
        for (RuleMethod.FactDeclaration p : factDeclarations) {
            this.args[p.position] = ctx.getObject(p.name);
        }
        if (contextParamId >= 0) {
            this.args[contextParamId] = ctx;
        }
        call();
    }
}
