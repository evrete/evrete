package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.collections.ArrayOf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

class TypeImpl<T> implements Type<T> {
    static final String THIS_FIELD_NAME = "this";
    private final int id;
    private final String name;
    private final Class<T> javaType;
    private final Map<String, TypeFieldImpl> fieldMap = new HashMap<>();
    private final ArrayOf<TypeFieldImpl> fieldsArray;

    TypeImpl(String name, int id, Class<T> javaType) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(javaType);
        this.javaType = javaType;
        this.name = name;
        this.id = id;

        this.fieldsArray = new ArrayOf<>(TypeFieldImpl.class);
        int thisIndex = 0;
        TypeFieldImpl thisField = new TypeFieldImpl(thisIndex, this, name, javaType, o -> o);
        this.fieldMap.put(THIS_FIELD_NAME, thisField);
        this.fieldsArray.set(thisIndex, thisField);
    }

    private TypeImpl(TypeImpl<T> other) {
        this(other.name, other.id, other.javaType);
        this.fieldMap.putAll(other.fieldMap);
        other.fieldsArray.forEach(new ObjIntConsumer<TypeFieldImpl>() {
            @Override
            public void accept(TypeFieldImpl typeField, int value) {
                TypeImpl.this.fieldsArray.set(value, typeField);
            }
        });
    }

    @Override
    public TypeField getField(int id) {
        return this.fieldsArray.get(id);
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
    public int getId() {
        return this.id;
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
    public TypeField getField(String name) {
        TypeField field = fieldMap.get(name);
        if (field == null) {
            synchronized (this) {
                field = fieldMap.get(name);
                if (field == null) {
                    field = inspectClass(name);
                }
            }
        }
        return field;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> TypeField declareField(String name, Class<V> type, Function<T, V> function) {
        return innerDeclare(name, type, o -> function.apply((T) o));
    }

    @Override
    public final Class<T> getJavaType() {
        return javaType;
    }

    @Override
    public final Collection<TypeField> getDeclaredFields() {
        return Collections.unmodifiableCollection(fieldMap.values());
    }

    @Override
    public String toString() {
        return "{name='" + name + '\'' +
                ", javaType=" + javaType +
                '}';
    }

    private synchronized TypeField innerDeclare(final String name, final Class<?> type, final Function<Object, ?> function) {
        Const.assertName(name);
        int newId = fieldMap.size();
        TypeFieldImpl field = new TypeFieldImpl(newId, this, name, type, function);
        this.fieldMap.put(name, field);
        this.fieldsArray.set(newId, field);
        return field;
    }

    private TypeField inspectClass(String dottedProp) {
        String[] parts = dottedProp.split("\\.");
        ArrayOf<ValueReader> getters = new ArrayOf<>(ValueReader.class);

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Class<?> valueType = javaType;
        for (String part : parts) {
            Const.assertName(part);
            ValueReader reader = resolve(lookup, valueType, part);
            if (reader == null) {
                return null;
            } else {
                valueType = reader.valueType();
                getters.append(reader);
            }
        }

        Function<Object, Object> func = getters.data.length == 1 ?
                new AtomicFunction(getters.data[0])
                :
                new NestedFunction(getters.data);


        return innerDeclare(dottedProp, valueType, func);
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
}
