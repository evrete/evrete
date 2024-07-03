package org.evrete.dsl;

import org.evrete.api.LhsField;
import org.evrete.api.TypeField;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

final class Utils {

    static boolean isDslRuleClass(Class<?> clazz) {
        if(clazz.getAnnotation(RuleSet.class) != null) return true;

        for(Method m : clazz.getMethods()) {
            if(m.getAnnotation(Rule.class) != null) return true;
        }
        return false;
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

    static Class<?>[] asMethodSignature(LhsField.Array<String, TypeField> references) {

        Class<?>[] signature = new Class<?>[references.length()];
        for (int i = 0; i < signature.length; i++) {
            TypeField field = references.get(i).field();
            signature[i] = field.getValueType();
        }
        return signature;
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
}
