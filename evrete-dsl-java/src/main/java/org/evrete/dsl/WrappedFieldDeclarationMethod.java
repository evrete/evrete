package org.evrete.dsl;

import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.FieldDeclaration;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Function;

class WrappedFieldDeclarationMethod<T, V> extends WrappedCloneableMethod<WrappedFieldDeclarationMethod<T, V>> {
    final Class<T> factJavaType;
    private final String factLogicalName;
    final String fieldName;
    private final Class<V> fieldType;

    @SuppressWarnings("unchecked")
    public WrappedFieldDeclarationMethod(WrappedClass declaringClass, Method delegate, FieldDeclaration ann) {
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

        AbstractDSLProvider.LOGGER.fine(() -> "New field declaration. Subject: '" + this.factLogicalName + "' (" + this.factJavaType.getName() + "), Field: '" + this.fieldName + "' (" + this.fieldType.getName() + "), Declaring method: " + delegate);
    }

    WrappedFieldDeclarationMethod(WrappedFieldDeclarationMethod<T, V> other, Object bindInstance) {
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
        selfRegister(resolver, new FailingFunction<>());
    }

    @Override
    WrappedFieldDeclarationMethod<T, V> bindTo(Object bindInstance) {
        return new WrappedFieldDeclarationMethod<>(this, bindInstance);
    }

    static class FailingFunction<T, V> implements Function<T, V> {
        @Override
        public V apply(T t) {
            throw new IllegalStateException("Field declaration not updated, please report the bug.");
        }
    }
}
