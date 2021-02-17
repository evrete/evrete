package org.evrete.runtime;

import org.evrete.api.Copyable;

class TypeMetaData implements Copyable<TypeMetaData> {
    TypeMetaData() {
    }

    private TypeMetaData(TypeMetaData other) {
    }


    @Override
    public TypeMetaData copyOf() {
        return new TypeMetaData(this);
    }
}
