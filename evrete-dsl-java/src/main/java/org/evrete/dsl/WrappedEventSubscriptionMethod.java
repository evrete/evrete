package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.Events;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class WrappedEventSubscriptionMethod<E extends ContextEvent> extends WrappedCloneableMethod<WrappedEventSubscriptionMethod<E>> {
    private final Class<E> eventClass;
    private final boolean async;

    @SuppressWarnings("unchecked")
    public WrappedEventSubscriptionMethod(WrappedClass declaringClass, Method delegate, boolean async) {
        super(declaringClass, delegate);
        this.async = async;

        if (!delegate.getReturnType().equals(void.class)) {
            throw new MalformedResourceException("Event subscription methods must be void: " + delegate);
        }

        Parameter[] parameters = delegate.getParameters();
        if (parameters.length != 1) {
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

    public WrappedEventSubscriptionMethod(WrappedEventSubscriptionMethod<E> other, Object bindInstance) {
        super(other, bindInstance);
        this.eventClass = other.eventClass;
        this.async = other.async;
    }

    void selfSubscribe(RuntimeContext<?> context) {
        Events.Subscription subscription = context.subscribe(this.eventClass, this.async, asVoidFunction());
        context.getService().getServiceSubscriptions().add(subscription);
    }

    @Override
    WrappedEventSubscriptionMethod<E> bindTo(Object bindInstance) {
        return new WrappedEventSubscriptionMethod<>(this, bindInstance);
    }
}
