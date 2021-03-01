package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Arrays;

final class MiscUtils {

    @SuppressWarnings("unused")
    public static Class<?> classForName(String className) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                String fqn = className.contains(".") ? className : "java.lang.".concat(className);
                try {
                    return Class.forName(fqn);
                } catch (ClassNotFoundException ex) {
                    throw new IllegalArgumentException("Class not found: " + fqn);
                }
        }
    }

    static int hash(MemoryKey[] v) {
        int h = 0, i = 0;
        for (; i < v.length; i++) {
            h = h ^ v[i].hashCode();
        }
        return h;
    }

    static MemoryKey[] toArray(IntToValueRow v, int size) {
        MemoryKey[] arr = new MemoryKey[size];
        for (int i = 0; i < size; i++) {
            arr[i] = v.apply(i);
        }
        return arr;
    }

    static boolean sameData(ValueHandle[] arr1, ValueHandle[] arr2) {
        return Arrays.equals(arr1, arr2);
    }
}
