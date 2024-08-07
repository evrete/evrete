package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

class TypeImpl<T> implements Type<T> {
    private static final ValueReader[] EMPTY_VALUE_READERS = new ValueReader[0];
    private final String name;
    private final Map<String, TypeFieldImpl> fieldMap = new HashMap<>();
    private final Class<T> javaType;

    TypeImpl(String name, Class<T> javaType) {
        Objects.requireNonNull(name);
        this.name = name;
        this.javaType = javaType;
    }

    private TypeImpl(TypeImpl<T> other) {
        this.fieldMap.putAll(other.fieldMap);
        this.name = other.name;
        this.javaType = other.javaType;
    }

    private static ValueReader resolve(MethodHandles.Lookup lookup, Class<?> clazz, String prop) {
        MethodHandle handle = null;

        // Scanning fields first
        for (Field field : clazz.getFields()) {
            if (field.getName().equals(prop)) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    try {
                        handle = lookup.unreflectGetter(field);
                        break;
                    } catch (IllegalAccessException e) {
                        // Field is not accessible, skipping
                    }
                }
            }
        }

        if (handle != null) {
            return new ValueReader(handle);
        }

        // Scanning methods
        for (MethodMeta meta : MethodMeta.values()) {
            String methodName = meta.buildName(prop);
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    if (meta.validMethod(method)) {
                        try {
                            handle = lookup.unreflect(method);
                            break;
                        } catch (IllegalAccessException e) {
                            // Method is not accessible, skipping
                        }
                    }
                }
            }

            if (handle != null) {
                return new ValueReader(handle);
            }
        }

        return null;
    }

    private static String capitalizeFirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public TypeImpl<T> copyOf() {
        return new TypeImpl<>(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeImpl<?> type = (TypeImpl<?>) o;
        return name.equals(type.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public @NonNull TypeField getField(@NonNull String name) {
        TypeField field = fieldMap.get(name);
        if (field == null) {
            synchronized (this) {
                field = fieldMap.get(name);
                if (field == null) {
                    field = resolveField(name);
                }
            }
        }

        if (field == null) {
            throw new IllegalArgumentException("Field not declared: '" + name + "'");
        } else {
            return field;
        }
    }

    @Override
    public Class<T> getJavaClass() {
        return javaType;
    }

    @Override
    public <V> TypeField declareField(String name, Class<V> type, Function<T, V> function) {
        return innerDeclare(name, type, new Func<>(function));
    }

    @Override
    @Deprecated
    public final String getJavaType() {
        return javaType.getName();
    }

    @Override
    @Deprecated
    public Class<T> resolveJavaType() {
        return javaType;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", fieldMap=" + fieldMap +
                ", javaType=" + javaType +
                ", zzz=" + System.identityHashCode(this) +
                '}';
    }

    private synchronized TypeField innerDeclare(final String name, final Class<?> type, final Function<Object, ?> function) {
        Const.assertName(name);
        TypeFieldImpl field = new TypeFieldImpl(name, this, type, function);
        this.fieldMap.put(name, field);
        return field;
    }

    private TypeField resolveField(String fieldName) {
        Function<Object, Object> func;
        Class<?> valueType;
        if (fieldName == null ||  fieldName.isEmpty()) {
            // "this" field
            valueType = javaType;
            func = o -> o;
        } else {
            String[] parts = fieldName.split("\\.");
            List<ValueReader> getters = new ArrayList<>();

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            valueType = javaType;
            for (String part : parts) {
                Const.assertName(part);
                ValueReader reader = resolve(lookup, valueType, part);
                if (reader == null) {
                    return null;
                } else {
                    valueType = reader.valueType();
                    getters.add(reader);
                }
            }

            func = getters.size() == 1 ?
                    new AtomicFunction(getters.get(0))
                    :
                    new NestedFunction(getters.toArray(EMPTY_VALUE_READERS));
        }
        return innerDeclare(fieldName, valueType, func);
    }

    private enum MethodMeta {
        DEFAULT("get", true, false),
        BOOL("is", true, true),
        RAW("", false, false);

        private final String prefix;
        private final boolean capitalizeFirst;
        private final boolean requireBoolean;

        MethodMeta(String prefix, boolean capitalizeFirst, boolean requireBoolean) {
            this.prefix = prefix;
            this.capitalizeFirst = capitalizeFirst;
            this.requireBoolean = requireBoolean;
        }

        String buildName(String prop) {
            if (capitalizeFirst) {
                return prefix + capitalizeFirst(prop);
            } else {
                return prefix + prop;
            }
        }

        boolean validMethod(Method method) {
            if (Modifier.isStatic(method.getModifiers())) return false;
            if (method.getParameterCount() > 0) return false;
            Class<?> retType = method.getReturnType();
            if (requireBoolean) {
                return retType.equals(boolean.class) || retType.equals(Boolean.class);
            } else {
                return !retType.equals(void.class);
            }
        }
    }

    private static class NestedFunction implements Function<Object, Object> {
        private final ValueReader[] readers;

        NestedFunction(ValueReader[] readers) {
            this.readers = readers;
        }

        @Override
        public Object apply(Object o) {
            try {
                Object current = o;
                for (ValueReader reader : readers) {
                    if ((current = reader.read(current)) == null) {
                        return null;
                    }
                }
                return current;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    private static class AtomicFunction implements Function<Object, Object> {
        private final ValueReader reader;

        AtomicFunction(ValueReader reader) {
            this.reader = reader;
        }

        @Override
        public Object apply(Object o) {
            try {
                return reader.read(o);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    private static final class ValueReader {
        private final MethodHandle handle;

        ValueReader(MethodHandle handle) {
            this.handle = handle;
        }

        Object read(Object o) throws Throwable {
            return handle.invoke(o);
        }

        Class<?> valueType() {
            return handle.type().returnType();
        }
    }

    private static class Func<T,V> implements Function<Object, Object> {
        private final Function<T, V> delegate;

        private Func(Function<T, V> function) {
            this.delegate = function;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object apply(Object o) {
            return delegate.apply((T) o);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
