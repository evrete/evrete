package org.evrete.runtime.builder;

import org.evrete.api.NamedType;
import org.evrete.api.TypeField;

public interface FieldReference {
    FieldReference[] ZERO_ARRAY = new FieldReference[0];

    TypeField field();

    NamedType type();
}
