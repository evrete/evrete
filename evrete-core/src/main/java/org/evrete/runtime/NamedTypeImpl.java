package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.annotations.NonNull;

class NamedTypeImpl implements NamedType {
    private final Type<?> type;
    private final String name;
    private final int index;

    public NamedTypeImpl(int index, @NonNull Type<?> type, String name) {
        this.index = index;
        this.type = type;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    @NonNull
    public Type<?> getType() {
        return type;
    }

    @Override
    public String getVarName() {
        return name;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
