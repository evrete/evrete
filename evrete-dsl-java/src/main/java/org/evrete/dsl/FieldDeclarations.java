package org.evrete.dsl;

import org.evrete.api.TypeResolver;

import java.util.HashMap;
import java.util.Map;

class FieldDeclarations {
    private final Map<String, FieldDeclarationMethod<?, ?>> fieldDeclarations = new HashMap<>();

    void addFieldDeclaration(FieldDeclarationMethod<?, ?> m) {
        String key = m.factType.getName() + "." + m.fieldName;
        if (fieldDeclarations.containsKey(key)) {
            throw new MalformedResourceException("Duplicate field definition " + key);
        } else {
            fieldDeclarations.put(key, m);
        }
    }


    void applyInitial(TypeResolver resolver) {
        for (FieldDeclarationMethod<?, ?> m : fieldDeclarations.values()) {
            m.applyInitial(resolver);
        }
    }

    void applyNormal(TypeResolver resolver, Object instance) {
        for (FieldDeclarationMethod<?, ?> m : fieldDeclarations.values()) {
            m.copy(instance).applyNormal(resolver);
        }
    }
}
