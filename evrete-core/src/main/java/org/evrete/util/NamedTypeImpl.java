package org.evrete.util;

import org.evrete.api.NamedType;
import org.evrete.api.Type;

public class NamedTypeImpl implements NamedType {
    private final Type<?> type;
    private final String name;

    public NamedTypeImpl(Type<?> type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public Type<?> getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }
}
