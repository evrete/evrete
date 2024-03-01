package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.annotations.NonNull;
import org.evrete.collections.ArrayOf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

class TypeImpl<T> implements Type<T> {
    private final int id;
    private final String name;
    private final ClassResolver classResolver;
    private final Map<String, TypeFieldImpl> fieldMap = new HashMap<>();
    private final String javaType;
    private final Supplier<Class<T>> classSupplier;

    TypeImpl(String name, String javaType, int id, Supplier<Class<T>> classSupplier) {
        Objects.requireNonNull(name);
        this.classSupplier = classSupplier;
        this.classResolver = new ClassResolver(classSupplier);
        this.name = name;
        this.javaType = javaType;
        this.id = id;
    }

    private TypeImpl(TypeImpl<T> other) {
        this.fieldMap.putAll(other.fieldMap);
        this.classSupplier = other.classSupplier;
        this.classResolver = new ClassResolver(this.classSupplier);
        this.name = other.name;
        this.javaType = other.javaType;
        this.id = other.id;
        for (Map.Entry<String, TypeFieldImpl> entry : other.fieldMap.entrySet()) {
            TypeFieldImpl f = entry.getValue().copy(this);
            save(f);
        }
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

    private void save(TypeFieldImpl f) {
        this.fieldMap.put(f.getName(), f);
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
    @SuppressWarnings("unchecked")
    public <V> TypeField declareField(String name, Class<V> type, Function<T, V> function) {
        return innerDeclare(name, type, o -> function.apply((T) o));
    }

    @Override
    public final String getJavaType() {
        return javaType;
    }

    @Override
    public Class<T> resolveJavaType() {
        return classResolver.get();
    }

    @Override
    public final Collection<TypeField> getDeclaredFields() {
        return Collections.unmodifiableCollection(fieldMap.values());
    }

    @Override
    public String toString() {
        return "{name='" + name + '\'' +
                '}';
    }

    private synchronized TypeField innerDeclare(final String name, final Class<?> type, final Function<Object, ?> function) {
        Const.assertName(name);
        TypeFieldImpl field = fieldMap.get(name);
        if (field == null) {
            field = new TypeFieldImpl(this, name, type, function);
            this.fieldMap.put(name, field);
        } else {
            field.setFunction(function);
        }

        return field;
    }

    private TypeField resolveField(@NonNull String fieldName) {
        Function<Object, Object> func;
        Class<?> valueType;
        if (fieldName.isEmpty()) {
            // "this" field
            valueType = classResolver.get();
            func = o -> o;
        } else {
            String[] parts = fieldName.split("\\.");
            ArrayOf<ValueReader> getters = new ArrayOf<>(ValueReader.class);

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            valueType = classResolver.get();
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

            func = getters.length() == 1 ?
                    new AtomicFunction(getters.get(0))
                    :
                    new NestedFunction(getters.getData());
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

    private class ClassResolver {
        private final Supplier<Class<T>> resolver;
        private volatile Class<T> resolved;

        ClassResolver(Supplier<Class<T>> resolver) {
            this.resolver = resolver;
        }

        Class<T> get() {
            if (resolved == null) {
                synchronized (this.resolver) {
                    if (resolved == null) {
                        resolved = resolver.get();
                        if (resolved == null) {
                            throw new IllegalStateException();
                        }
                    }
                }
            }
            return resolved;
        }
    }

}
