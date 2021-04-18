package org.evrete.dsl;

import org.evrete.dsl.annotation.RuleSet;

public final class Utils {

    static RuleSet.Sort deriveSort(Class<?> clazz) {
        RuleSet.Sort sort = searchSort(clazz);
        return sort == null ? RuleSet.Sort.DEFAULT : sort;
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
