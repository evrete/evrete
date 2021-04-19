package org.evrete.dsl;

import org.evrete.dsl.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class Utils {

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

    private static boolean hasDslAnnotation(Method m) {
        return m.getAnnotation(Rule.class) != null
                ||
                m.getAnnotation(PhaseListener.class) != null
                ||
                m.getAnnotation(Where.class) != null
                ||
                m.getAnnotation(FieldDeclaration.class) != null
                ;
    }

    static RuleSet.Sort deriveSort(Class<?> clazz) {
        RuleSet.Sort sort = searchSort(clazz);
        return sort == null ? RuleSet.Sort.DEFAULT : sort;
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


    public static Class<?> box(Class<?> type) {
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
}
