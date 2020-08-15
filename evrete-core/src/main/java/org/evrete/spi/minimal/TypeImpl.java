package org.evrete.spi.minimal;

import org.evrete.api.Copyable;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.collections.ArrayOf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class TypeImpl implements Copyable<TypeImpl>, Type {
    private final String name;
    private final Class<?> clazz;
    private final Map<String, TypeFieldImpl> fields = new HashMap<>();
    private final JcCompiler compiler;

    private TypeImpl(JcCompiler compiler, Class<?> clazz) {
        this(clazz.getName(), clazz, compiler);
    }

    private TypeImpl(String name, Class<?> clazz, JcCompiler compiler) {
        this.clazz = clazz;
        this.compiler = compiler;
        this.name = name;
    }

    private TypeImpl(String name, JcCompiler compiler) {
        this(name, null, compiler);
    }

    private TypeImpl(TypeImpl other) {
        this(other.name, other.clazz, other.compiler);
        this.fields.putAll(other.fields);
    }


    static TypeImpl factory(JcCompiler compiler, String className) {
        Const.assertName(className);
        Class<?> clazz;
        try {
            ClassLoader classLoader = compiler.getClassLoader();
            clazz = classLoader.loadClass(className);
            return new TypeImpl(compiler, clazz);
        } catch (ClassNotFoundException e) {
            return new TypeImpl(className, compiler);
        }
    }

    @Override
    public TypeImpl copyOf() {
        return new TypeImpl(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeImpl type = (TypeImpl) o;
        return name.equals(type.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    static ValueReader resolve(MethodHandles.Lookup lookup, Class<?> clazz, String prop) {
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

    @Override
    public TypeField getField(String name) {
        return fields.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TypeField declareField(String name, Class<?> type, String lambdaExpression) {
        Function<Object, Object> function = (Function<Object, Object>) compiler.compileLambda(this, lambdaExpression);
        return declareField(name, type, function);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeField declareField(String name, Class<?> type, Function<T, Object> function) {
        return getCreateField(name, type, o -> function.apply((T) o));
    }

    final Class<?> getClazz() {
        return clazz;
    }

    @Override
    public final Collection<TypeField> getDeclaredFields() {
        return Collections.unmodifiableCollection(fields.values());
    }


    @Override
    public String toString() {
        return clazz.getSimpleName();
    }

    private TypeField getCreateField(final String name, final Class<?> type, final Function<Object, ?> function) {
        Const.assertName(name);
        TypeFieldImpl field = fields.get(name);
        if (field == null) {
            synchronized (fields) {
                field = fields.get(name);
                if (field == null) {
                    field = new TypeFieldImpl(this, name, type, function);
                    this.fields.put(name, field);
                }
            }
        }
        return field;
    }

    public static String capitalizeFirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    synchronized TypeField inspectClass(String dottedProp) {
        TypeField field = getField(dottedProp);
        if (field != null) return field;
        Class<?> currentClass = clazz;
        if (currentClass == null) return null;
        String[] parts = dottedProp.split("\\.");
        ArrayOf<ValueReader> getters = new ArrayOf<>(ValueReader.class);

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Class<?> valueType = currentClass;
        for (String part : parts) {
            ValueReader reader = resolve(lookup, currentClass, part);
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


        return getCreateField(dottedProp, valueType, func);
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