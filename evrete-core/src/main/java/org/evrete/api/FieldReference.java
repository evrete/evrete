package org.evrete.api;

public interface FieldReference {
    FieldReference[] ZERO_ARRAY = new FieldReference[0];

    TypeField field();

    NamedType type();
}
