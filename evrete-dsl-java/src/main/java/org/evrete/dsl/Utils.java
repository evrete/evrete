package org.evrete.dsl;

import org.evrete.api.FieldReference;
import org.evrete.api.Type;
import org.evrete.dsl.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Utils {
    static final Logger LOGGER = Logger.getLogger(Utils.class.getPackage().getName());


    //TODO remove this double scan
    static Collection<Method> allNonPublicAnnotated(Class<?> clazz) {
        Class<?> current = clazz;
        Set<Method> methods = new HashSet<>();
        if (clazz.equals(Object.class)) return methods;

        while (!current.equals(Object.class)) {
            for (Method m : current.getDeclaredMethods()) {
                if (hasDslAnnotation(m) && !Modifier.isPublic(m.getModifiers())) {
                    methods.add(m);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    static String factName(Parameter parameter) {
        Fact fact = parameter.getAnnotation(Fact.class);
        if (fact != null) {
            return fact.value();
        } else {
            return parameter.getName();
        }
    }

    static String factType(Parameter parameter) {
        Fact fact = parameter.getAnnotation(Fact.class);
        if (fact == null || fact.type().isEmpty()) {
            return null;
        } else {
            return fact.type();
        }
    }

    static Class<?>[] asMethodSignature(FieldReference[] references) {
        Class<?>[] signature = new Class<?>[references.length];
        for (int i = 0; i < references.length; i++) {
            String fieldName = references[i].field();
            Type<?> namedType = references[i].type().getType();
            signature[i] = namedType.getField(fieldName).getValueType();
        }
        return signature;
    }

    private static boolean hasDslAnnotation(Method m) {
        return m.getAnnotation(Rule.class) != null
                ||
                m.getAnnotation(PhaseListener.class) != null
                ||
                m.getAnnotation(Where.class) != null
                ||
                m.getAnnotation(FieldDeclaration.class) != null
                ||
                m.getAnnotation(EnvironmentListener.class) != null
                ;
    }

    static RuleSet.Sort deriveSort(Class<?> clazz) {
        RuleSet.Sort sort = searchSort(clazz);
        return sort == null ? RuleSet.Sort.BY_NAME : sort;
    }

    static String ruleName(Method method) {
        Rule ruleAnn = Objects.requireNonNull(method.getAnnotation(Rule.class));
        String name = ruleAnn.value().trim();
        if (name.isEmpty()) {
            return method.getName();
        } else {
            return name;
        }
    }

    static int salience(Method method) {
        return Objects.requireNonNull(method.getAnnotation(Rule.class)).salience();
    }

    private static RuleSet.Sort searchSort(Class<?> clazz) {
        RuleSet policy = clazz.getAnnotation(RuleSet.class);
        if (policy != null) {
            return policy.defaultSort();
        } else {
            Class<?> parent = clazz.getSuperclass();
            if (parent.equals(Object.class)) {
                return null;
            } else {
                return searchSort(parent);
            }
        }
    }


    static Class<?> box(Class<?> type) {
        if (type.isPrimitive()) {
            // Primitive types can not be used as fact types and need to be boxed
            switch (type.getName()) {
                case "boolean":
                    return Boolean.class;
                case "byte":
                    return Byte.class;
                case "short":
                    return Short.class;
                case "int":
                    return Integer.class;
                case "long":
                    return Long.class;
                case "float":
                    return Float.class;
                case "double":
                    return Double.class;
                case "char":
                    return Character.class;
                case "void":
                    return Void.class;
                default:
                    throw new IllegalStateException();
            }
        } else {
            return type;
        }
    }

    static Stream<JarBytesEntry> jarStream(JarInputStream stream) {
        return StreamSupport.stream(jarIterator(stream), false)
                .onClose(() -> {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .filter(wrapper -> wrapper.bytesEntry != null)
                .map(wrapper -> wrapper.bytesEntry);
    }

    private static Spliterator<JarEntryWrapper> jarIterator(JarInputStream stream) {
        return Spliterators.spliterator(new JarIterator(stream), Long.MAX_VALUE, Spliterator.IMMUTABLE);
    }

    private static class JarIterator implements Iterator<JarEntryWrapper> {
        private final JarInputStream stream;
        private JarEntryWrapper entry;
        private final byte[] buffer = new byte[4096];

        JarIterator(JarInputStream stream) {
            this.stream = stream;
            this.advance();
        }

        private void advance() {
            try {
                JarEntry next = stream.getNextJarEntry();
                if(next == null) {
                    entry = null;
                } else {
                    if(next.isDirectory()) {
                        entry = new JarEntryWrapper(next, null);
                    } else {
                        byte[] bytes = toBytes(stream, this.buffer);
                        entry = new JarEntryWrapper(next, new JarBytesEntry(next.getName(), bytes));
                    }
                }
            } catch (IOException e) {
                try {
                    stream.close();
                    throw new UncheckedIOException(e);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        private static byte[] toBytes(JarInputStream is, byte[] buffer) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
            bos.close();
            return bos.toByteArray();
        }


        @Override
        public boolean hasNext() {
            return entry != null;
        }

        @Override
        public JarEntryWrapper next() {
            JarEntryWrapper result = entry;
            advance();
            return result;
        }
    }


    private static class JarEntryWrapper {
        final JarEntry entry;
        final JarBytesEntry bytesEntry;

        JarEntryWrapper(JarEntry entry, JarBytesEntry bytesEntry) {
            this.entry = entry;
            this.bytesEntry = bytesEntry;
        }
    }
}
