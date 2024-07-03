package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

class DefaultNamedTypeResolver<T extends NamedType> implements NamedType.Resolver {
    private final LinkedHashMap<String, T> map = new LinkedHashMap<>();

    @NonNull
    @Override
    public T resolve(@NonNull String var) {
        T t = map.get(var);
        if (t == null) {
            throw new NoSuchElementException("No type registered with variable '" + var + "'");
        } else {
            return t;
        }
    }

    protected int size() {
        return map.size();
    }

    public void save(T value) {
        T prev = map.put(value.getVarName(), value);
        if (prev != null) {
            throw new IllegalArgumentException("Duplicate type reference '" + value.getVarName() + "'");
        }
    }

    Collection<T> rawValues() {
        return map.values();
    }

    public final Collection<NamedType> getDeclaredFactTypes() {
        return Collections.unmodifiableCollection(rawValues());
    }
}
