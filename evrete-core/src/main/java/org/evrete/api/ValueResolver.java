package org.evrete.api;

public interface ValueResolver {
    ValueHandle getValueHandle(Class<?> valueType, Object value);

    Object getValue(ValueHandle handle);

}
