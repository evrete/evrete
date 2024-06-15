package org.evrete.dsl;

import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.*;
import org.evrete.util.ArrayOf;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.logging.Logger;

class RulesClass extends WrappedClass {
    private static final Logger LOGGER = Logger.getLogger(RulesClass.class.getName());
    final RuleMethods ruleMethods = new RuleMethods();
    final FieldDeclarationMethods fieldDeclarationMethods = new FieldDeclarationMethods();
    final EnvListenerMethods envListenerMethods = new EnvListenerMethods();
    final PhaseListenerMethods phaseListenerMethods = new PhaseListenerMethods();

    RulesClass(WrappedClass other, MethodHandles.Lookup publicLookup) {
        super(other);

        MethodHandles.Lookup classLookup = publicLookup.in(this.delegate);

        for (Method m : this.publicMethods) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EnvironmentListener envListener = m.getAnnotation(EnvironmentListener.class);
            PhaseListener phaseListener = m.getAnnotation(PhaseListener.class);

            // The annotations above are mutually exclusive.
            if (ruleAnnotation != null) {
                this.ruleMethods.add(new RuleMethod(classLookup, m, ruleAnnotation));
            } else if (fieldDeclaration != null) {
                this.fieldDeclarationMethods.add(new FieldDeclarationMethod(classLookup, m, fieldDeclaration));
            } else if (envListener != null) {
                String property = envListener.value();
                if (property.isEmpty()) {
                    LOGGER.warning(()->"The @" + EnvironmentListener.class.getSimpleName() + " annotation on " + m + " has no property value and will be ignored");
                } else {
                    this.envListenerMethods.add(new EnvListenerMethod(classLookup, m, envListener));
                }
            } else if (phaseListener != null) {
                this.phaseListenerMethods.add(new PhaseListenerMethod(classLookup, m, phaseListener));
            }
        }
    }

    static class RuleMethods  {
        ArrayOf<RuleMethod> methods = new ArrayOf<>(RuleMethod.class);
        void add(RuleMethod method) {
            this.methods.append(method);
        }
    }

    static class FieldDeclarationMethods  {
        ArrayOf<FieldDeclarationMethod> methods = new ArrayOf<>(FieldDeclarationMethod.class);
        void add(FieldDeclarationMethod method) {
            this.methods.append(method);
        }
    }

    static class EnvListenerMethods  {
        void add(EnvListenerMethod method) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static class PhaseListenerMethods  {
        void add(PhaseListenerMethod method) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static class RuleMethod extends WrappedMethod {
        final Where where;
        final Rule rule;

        RuleMethod(MethodHandles.Lookup lookup, Method delegate, Rule ruleAnnotation) {
            super(lookup, delegate);
            this.rule = ruleAnnotation;
            this.where = delegate.getAnnotation(Where.class);
        }
    }

    static class FieldDeclarationMethod extends WrappedMethod {
        private final FieldDeclaration annotation;
        private final Parameter parameter;

        public FieldDeclarationMethod(MethodHandles.Lookup lookup, Method delegate, FieldDeclaration fieldDeclaration) {
            super(lookup, delegate);
            this.annotation = fieldDeclaration;

            if(this.parameters.length != 1) {
                throw new IllegalArgumentException("FieldDeclaration method must have exactly one parameter. Failed method: " + delegate);
            } else {
                this.parameter = this.parameters[0];
            }
        }

        void selfRegister(TypeResolver typeResolver) {

        }

        String logicalType() {
            return annotation.type();
        }
    }

    static class EnvListenerMethod extends WrappedMethod {
        public EnvListenerMethod(MethodHandles.Lookup lookup, Method delegate, EnvironmentListener envListener) {
            super(lookup, delegate);
        }
    }

    static class PhaseListenerMethod extends WrappedMethod {
        public PhaseListenerMethod(MethodHandles.Lookup lookup, Method delegate, PhaseListener phaseListener) {
            super(lookup, delegate);
        }
    }
}
