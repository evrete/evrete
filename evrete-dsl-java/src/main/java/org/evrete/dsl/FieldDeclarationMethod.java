package org.evrete.dsl;

import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.FieldDeclaration;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

class FieldDeclarationMethod<T, V> extends ClassMethod implements SessionCloneable<FieldDeclarationMethod<T, V>> {
    final Class<T> factType;
    final String fieldName;
    private final Class<V> fieldType;

    @SuppressWarnings("unchecked")
    FieldDeclarationMethod(MethodHandles.Lookup lookup, Method method) {
        super(lookup, method);
        FieldDeclaration ann = Objects.requireNonNull(method.getAnnotation(FieldDeclaration.class));
        String declaredName = ann.name().trim();
        if (declaredName.isEmpty()) {
            this.fieldName = method.getName();
        } else {
            this.fieldName = declaredName;
        }

        this.fieldType = (Class<V>) method.getReturnType();
        if (fieldType.equals(void.class) || fieldType.equals(Void.class)) {
            throw new MalformedResourceException("Method " + method + " in the " + method.getDeclaringClass() + " is annotated as field declaration but is void");
        }

        Parameter[] parameters = method.getParameters();
        if (parameters.length != 1) {
            throw new MalformedResourceException("Method " + method + " in the " + method.getDeclaringClass() + " is annotated as field declaration but has zero or more than one parameters");
        }

        this.factType = (Class<T>) parameters[0].getType();
    }

    private FieldDeclarationMethod(FieldDeclarationMethod<T, V> method, Object instance) {
        super(method, instance);
        this.fieldType = method.fieldType;
        this.factType = method.factType;
        this.fieldName = method.fieldName;
    }

    @Override
    public FieldDeclarationMethod<T, V> copy(Object sessionInstance) {
        return new FieldDeclarationMethod<>(this, sessionInstance);
    }

    void applyNormal(TypeResolver resolver) {
        resolver.getOrDeclare(factType).declareField(fieldName, fieldType, asFunction());
    }

    void applyInitial(TypeResolver resolver) {
        resolver.getOrDeclare(factType).declareField(fieldName, fieldType, t -> {
            throw new IllegalStateException();
        });
    }


}
