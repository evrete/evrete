package org.evrete.dsl;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class RuleMethod  {
    final Rule rule;
    final String[] literalConditions;
    final MethodPredicate[] methodPredicates;
    final RhsMethod rhs;

    private final String ruleName;

    RuleMethod(WrappedClass declaringClass, Method delegate, Rule ruleAnnotation) {
        this.rhs = new RhsMethod(declaringClass, delegate);
        this.rule = ruleAnnotation;
        this.ruleName = Utils.ruleName(delegate);
        Where predicates = delegate.getAnnotation(Where.class);
        if (predicates == null) {
            this.literalConditions = new String[0];
            this.methodPredicates = new MethodPredicate[0];
        } else {
            this.literalConditions = predicates.value();
            this.methodPredicates = predicates.methods();
        }
    }

    String getRuleName() {
        return ruleName;
    }

    int getSalience() {
        return rule.salience();
    }

    static class FactDeclaration {
        static final FactDeclaration[] EMPTY = new FactDeclaration[0];
        final int position;
        final String name;
        final Class<?> javaType;
        final String logicalType;

        FactDeclaration(Parameter parameter, int position) {
            this.name = Utils.factName(parameter);
            this.position = position;
            this.javaType = parameter.getType();
            this.logicalType = Utils.factType(parameter);
        }
    }

    static class RhsMethod extends WrappedMethod implements Consumer<RhsContext> {
        final FactDeclaration[] factDeclarations;
        private final int contextParamId;

        public RhsMethod(WrappedClass declaringClass, Method delegate) {
            super(declaringClass, delegate);

            int ctxIndex = Integer.MIN_VALUE;
            if (!delegate.getReturnType().equals(void.class)) {
                throw new MalformedResourceException("Rule methods must be void: " + delegate);
            }

            List<FactDeclaration> rhsParameterList = new ArrayList<>();
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
                    FactDeclaration rhsParameter = new FactDeclaration(param, i);
                    rhsParameterList.add(rhsParameter);
                }
            }
            this.factDeclarations = rhsParameterList.toArray(FactDeclaration.EMPTY);
            this.contextParamId = ctxIndex;
        }

        public RhsMethod(RhsMethod other, Object bindInstance) {
            super(other, bindInstance);
            this.factDeclarations = other.factDeclarations;
            this.contextParamId = other.contextParamId;
        }

        RhsMethod bindTo(Object classInstance) {
            return new RhsMethod(this, classInstance);
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
    }
}
