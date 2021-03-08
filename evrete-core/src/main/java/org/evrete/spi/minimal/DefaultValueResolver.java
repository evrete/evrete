package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class DefaultValueResolver implements ValueResolver {
    private static final ValueHandle NULL_HANDLE = new ValueHandleImpl(new int[]{-1, -1});
    private static final int INITIAL_TYPE_DATA_SIZE = 128;
    private final Map<String, Integer> typeIndices = new HashMap<>();
    private final AtomicInteger typeIdCounter = new AtomicInteger(0);
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
        return impl.data[0] < 0 ? null : typeDataArr[impl.data[0]].values[impl.data[1]];
    }

    synchronized private ValueHandle getValueHandleInner(Class<?> valueType, Object value) {
        String typeKey = valueType.getName();
        Integer typeIdx = typeIndices.get(typeKey);
        TypeData typeData;
        if (typeIdx == null) {
            typeIdx = typeIdCounter.getAndIncrement();
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
        private final AtomicInteger idCounter = new AtomicInteger();
        private final int id;
        private Object[] values;

        TypeData(int id) {
            this.id = id;
            this.values = new Object[INITIAL_VALUE_DATA_SIZE];
        }

        ValueHandleImpl getHandle(Object value) {
            ValueHandleImpl handle = idMap.get(value);
            if (handle == null) {
                int valueId = idCounter.getAndIncrement();
                handle = new ValueHandleImpl(new int[]{id, valueId});
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
