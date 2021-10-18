package org.evrete.api;

public interface FieldReference {
    FieldReference[] ZERO_ARRAY = new FieldReference[0];

    static boolean sameAs(FieldReference[] refs1, FieldReference[] refs2) {
        if (refs1.length != refs2.length) return false;

        for (int i = 0; i < refs1.length; i++) {
            if (!refs1[i].sameAs(refs2[i])) {
                return false;
            }
        }
        return true;
    }

    TypeField field();

    NamedType type();

    default boolean sameAs(FieldReference other) {
        return other.field().getName().equals(field().getName()) && other.type().sameAs(type());
    }
}
