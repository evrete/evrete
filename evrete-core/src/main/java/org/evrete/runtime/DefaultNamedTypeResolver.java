package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.api.annotations.NonNull;

import java.util.HashMap;
import java.util.NoSuchElementException;

public class DefaultNamedTypeResolver<T extends NamedType> extends HashMap<String, T> implements NamedType.Resolver {
    @NonNull
    @Override
    public NamedType resolve(@NonNull String var) {
        T t = get(var);
        if (t == null) {
            throw new NoSuchElementException("No type registered with variable '" + var + "'");
        } else {
            return t;
        }
    }

    @Override
    public T put(String key, T value) {
        T prev = super.put(key, value);
        if (prev == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Duplicate type reference '" + key + "'");
        }
    }
}
