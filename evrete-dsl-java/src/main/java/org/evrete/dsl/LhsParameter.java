package org.evrete.dsl;

import org.evrete.dsl.annotation.Fact;

import java.lang.reflect.Parameter;

class LhsParameter {
    static LhsParameter[] EMPTY_ARRAY = new LhsParameter[0];
    final int position;
    final String lhsRef;
    private final String factType;

    LhsParameter(Parameter parameter, int position) {
        String ref;
        Fact fact = parameter.getAnnotation(Fact.class);
        if (fact != null) {
            ref = fact.value();
        } else {
            ref = parameter.getName();
        }

        if (!ref.startsWith("$")) {
            throw new MalformedResourceException("LHS parameter name '" + parameter.getName() + "' in " + parameter.getDeclaringExecutable().getName() + " can not be correctly derived from Java class meta-data or does not start with '$'. Use the @Fact parameter annotation to specify the correct fact type name.");
        } else {
            this.position = position;
            this.lhsRef = ref;
            Class<?> parameterType = lhsType(parameter);
            this.factType = parameterType.getName();
        }
    }

    private static Class<?> lhsType(Parameter parameter) {
        Class<?> type = parameter.getType();
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

    String getLhsRef() {
        return lhsRef;
    }

    String getFactType() {
        return factType;
    }
}
