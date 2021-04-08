package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;
import org.evrete.util.NextIntSupplier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class DefaultValueResolver implements ValueResolver {
    private static final ValueHandle NULL_HANDLE = new ValueHandleImpl(-1, -1);
    private static final int INITIAL_TYPE_DATA_SIZE = 128;
    private final Map<String, Integer> typeIndices = new HashMap<>();
    private final NextIntSupplier typeIdCounter = new NextIntSupplier();
    private TypeData[] typeDataArr;

    DefaultValueResolver() {
        this.typeDataArr = new TypeData[INITIAL_TYPE_DATA_SIZE];
    }

    @Override
    public ValueHandle getValueHandle(Class<?> valueType, Object value) {
        return value == null ? NULL_HANDLE : getValueHandleInner(valueType, value);
    }

    @Override
    public Object getValue(ValueHandle handle) {
        ValueHandleImpl impl = (ValueHandleImpl) handle;
        return impl.typeId < 0 ? null : typeDataArr[impl.typeId].values[impl.valueId];
    }

    synchronized private ValueHandle getValueHandleInner(Class<?> valueType, Object value) {
        String typeKey = valueType.getName();
        Integer typeIdx = typeIndices.get(typeKey);
        TypeData typeData;
        if (typeIdx == null) {
            typeIdx = typeIdCounter.next();
            typeData = new TypeData(typeIdx);
            if (typeIdx >= typeDataArr.length) {
                typeDataArr = Arrays.copyOf(typeDataArr, typeDataArr.length * 2);
            }
            typeDataArr[typeIdx] = typeData;
            typeIndices.put(typeKey, typeIdx);
            return typeData.getHandle(value);
        }
        return typeDataArr[typeIdx].getHandle(value);
    }

    private static class TypeData {
        private static final int INITIAL_VALUE_DATA_SIZE = 1024;
        private final Map<Object, ValueHandleImpl> idMap = new HashMap<>();
        private final NextIntSupplier idCounter = new NextIntSupplier();
        private final int id;
        private Object[] values;

        TypeData(int id) {
            this.id = id;
            this.values = new Object[INITIAL_VALUE_DATA_SIZE];
        }

        ValueHandleImpl getHandle(Object value) {
            ValueHandleImpl handle = idMap.get(value);
            if (handle == null) {
                int valueId = idCounter.next();
                handle = new ValueHandleImpl(id, valueId);
                idMap.put(value, handle);

                if (valueId >= values.length) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[valueId] = value;
            }
            return handle;
        }
    }
}
