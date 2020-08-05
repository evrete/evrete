package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.ValueRow;

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


    static boolean sameData(ValueRow[] arr1, ValueRow[] arr2) {
        int l = arr1.length;
        assert l == arr2.length;

        for (int i = 0; i < l; i++) {
            if (arr1[i] != arr2[i]) return false;
        }
        return true;
    }

    static int hash(ValueRow[] v) {
        int h = 0, i = 0;
        for (; i < v.length; i++) {
            h = h ^ v[i].hashCode();
        }
        return h;
    }

    static ValueRow[] toArray(IntToValueRow v, int size) {
        ValueRow[] arr = new ValueRow[size];
        for (int i = 0; i < size; i++) {
            arr[i] = v.apply(i);
        }
        return arr;
    }

    static int hash(IntToValueRow v, int size) {
        int h = 0, i = 0;
        for (; i < size; i++) {
            h = h ^ v.apply(i).hashCode();
        }
        return h;
    }

    static boolean eqIdentity(IntToValueRow v, ValueRow[] arr) {
        int i = 0;
        for (; i < arr.length; i++) {
            if (v.apply(i) != arr[i]) return false;
        }
        return true;
    }

    static boolean sameData(Object[] arr1, Object[] arr2) {
        return Arrays.equals(arr1, arr2);
    }
}
