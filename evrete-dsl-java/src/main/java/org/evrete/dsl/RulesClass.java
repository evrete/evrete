package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.api.events.ContextEvent;
import org.evrete.dsl.annotation.EventSubscription;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.Rule;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

class RulesClass extends WrappedClass {
    final List<RuleMethod> ruleMethods = new LinkedList<>();
    final List<FieldDeclarationMethod<?,?>> fieldDeclarationMethods = new LinkedList<>();
    final List<EventSubscriptionMethod<?>> subscriptionMethods = new LinkedList<>();

    RulesClass(WrappedClass other) {
        super(other);

        for (Method m : this.publicMethods) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EventSubscription eventSubscription = m.getAnnotation(EventSubscription.class);
            // The annotations above are mutually exclusive.
            if (ruleAnnotation != null) {
                this.ruleMethods.add(new RuleMethod(this, m, ruleAnnotation));
            } else if (fieldDeclaration != null) {
                this.fieldDeclarationMethods.add(new FieldDeclarationMethod<>(this, m, fieldDeclaration));
            } else if (eventSubscription != null) {
                this.subscriptionMethods.add(new EventSubscriptionMethod<>(this, m, eventSubscription.async()));
            }
        }
    }


    static class FieldDeclarationMethod<T, V> extends WrappedMethod {
        final Class<T> factJavaType;
        private final String factLogicalName;
        final String fieldName;
        private final Class<V> fieldType;

        @SuppressWarnings("unchecked")
        public FieldDeclarationMethod(WrappedClass declaringClass, Method delegate, FieldDeclaration ann) {
            super(declaringClass, delegate);

            // 1. Get the field's name
            String declaredName = ann.name().trim();
            if (declaredName.isEmpty()) {
                this.fieldName = delegate.getName();
            } else {
                this.fieldName = declaredName;
            }

            // 2. Get the field's type
            this.fieldType = (Class<V>) delegate.getReturnType();
            if (fieldType.equals(void.class) || fieldType.equals(Void.class)) {
                throw new MalformedResourceException("Method " + delegate + " in the " + delegate.getDeclaringClass() + " is annotated as field declaration but is void");
            }

            // 3. Get the fact's Java type
            Parameter[] parameters = delegate.getParameters();
            if (parameters.length != 1) {
                throw new IllegalArgumentException("FieldDeclaration method must have exactly one parameter. Failed method: " + delegate);
            }
            String typeName = ann.type();
            this.factJavaType = (Class<T>) parameters[0].getType();

            // 4. Get the fact's logical type
            this.factLogicalName = typeName == null || typeName.isEmpty() ? Type.logicalNameOf(this.factJavaType) : typeName;
        }

         FieldDeclarationMethod(FieldDeclarationMethod<T,V> other, Object bindInstance) {
            super(other, bindInstance);
            this.factLogicalName = other.factLogicalName;
            this.factJavaType = other.factJavaType;
            this.fieldName = other.fieldName;
            this.fieldType = other.fieldType;
        }

        void selfRegister(TypeResolver resolver) {
            selfRegister(resolver, asFunction());
        }

        private void selfRegister(TypeResolver resolver, Function<T, V> fieldFunction) {
            resolver.getOrDeclare(factLogicalName, factJavaType).declareField(fieldName, fieldType, fieldFunction);
        }

        void dummyRegister(TypeResolver resolver) {
            selfRegister(resolver, t -> {
                throw new IllegalStateException("Field declaration not updated, please report the bug.");
            });
        }

        FieldDeclarationMethod<T,V> bindTo(Object bindInstance) {
            return new FieldDeclarationMethod<>(this, bindInstance);
        }
    }

    static class EventSubscriptionMethod<E extends ContextEvent> extends WrappedMethod {
        private final Class<E> eventClass;
        private final boolean async;

        @SuppressWarnings("unchecked")
        public EventSubscriptionMethod(WrappedClass declaringClass, Method delegate, boolean async) {
            super(declaringClass, delegate);
            this.async = async;

            if (!delegate.getReturnType().equals(void.class)) {
                throw new MalformedResourceException("Event subscription methods must be void: " + delegate);
            }

            Parameter[] parameters = delegate.getParameters();
            if(parameters.length != 1) {
                throw new MalformedResourceException("Event subscription methods must have exactly one argument: " + delegate);
            }

            Parameter parameter = parameters[0];
            Class<?> parameterType = parameter.getType();
            if (ContextEvent.class.isAssignableFrom(parameterType)) {
                this.eventClass = (Class<E>) parameterType;
            } else {
                throw new MalformedResourceException("Invalid event subscription method argument: " + parameter);
            }
        }

        public EventSubscriptionMethod(EventSubscriptionMethod<E> other, Object bindInstance) {
            super(other, bindInstance);
            this.eventClass = other.eventClass;
            this.async = other.async;
        }

        void selfSubscribe(RuntimeContext<?> context) {
            Events.Subscription subscription = context.subscribe(this.eventClass, this.async, asVoidFunction());
            context.getService().getServiceSubscriptions().add(subscription);
        }

        EventSubscriptionMethod<E> bindTo(Object bindInstance) {
            return new EventSubscriptionMethod<>(this, bindInstance);
        }
    }

    static class Condition extends WrappedMethod implements ValuesPredicate {
        public Condition(WrappedMethod other) {
            super(other);
        }

        public Condition(WrappedMethod other, Object bindInstance) {
            super(other, bindInstance);
        }

        @Override
        public boolean test(IntToValue values) {
            for (int i = 0; i < args.length; i++) {
                this.args[i] = values.apply(i);
            }
            return call();
        }

        public Condition bindTo(Object instance) {
            return new Condition(this, instance);
        }
    }
}
