package org.evrete.spi.minimal;

import org.evrete.api.FieldValue;
import org.evrete.api.ValueResolver;

class DefaultValueResolver implements ValueResolver {

    @Override
    public FieldValueImpl getValueHandle(Class<?> valueType, Object value) {
        return new FieldValueImpl(value);
    }

    @Override
    public Object getValue(FieldValue handle) {
        FieldValueImpl impl = (FieldValueImpl) handle;
        return impl.value;
    }
}
