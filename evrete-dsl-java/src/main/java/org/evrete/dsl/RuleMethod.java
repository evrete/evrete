package org.evrete.dsl;

import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class RuleMethod  {
    final Rule rule;
    final String[] literalConditions;
    final MethodPredicate[] methodPredicates;
    final WrappedRhsMethod rhs;

    private final String ruleName;

    RuleMethod(WrappedClass declaringClass, Method delegate, Rule ruleAnnotation) {
        this.rhs = new WrappedRhsMethod(declaringClass, delegate);
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

}
