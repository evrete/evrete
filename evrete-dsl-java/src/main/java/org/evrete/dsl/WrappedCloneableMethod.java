package org.evrete.dsl;

import org.evrete.api.annotations.NonNull;

import java.lang.reflect.Method;

abstract class WrappedCloneableMethod<M extends WrappedCloneableMethod<M>> extends WrappedMethod {

    abstract M bindTo(Object classInstance);

    WrappedCloneableMethod(WrappedMethod other, Object bindInstance) {
        super(other, bindInstance);
    }

    WrappedCloneableMethod(WrappedMethod other) {
        super(other);
    }

    WrappedCloneableMethod(WrappedClass declaringClass, @NonNull Method delegate) {
        super(declaringClass, delegate);
    }
}
