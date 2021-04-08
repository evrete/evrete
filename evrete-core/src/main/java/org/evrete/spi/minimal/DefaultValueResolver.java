package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;

class DefaultValueResolver implements ValueResolver {

    DefaultValueResolver() {
    }

    @Override
    public ValueHandleImpl getValueHandle(Class<?> valueType, Object value) {
        return new ValueHandleImpl(value);
    }

    @Override
    public Object getValue(ValueHandle handle) {
        ValueHandleImpl impl = (ValueHandleImpl) handle;
        return impl.value;
    }
}
