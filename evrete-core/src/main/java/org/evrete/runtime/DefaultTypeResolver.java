package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTypeResolver implements NamedType.Resolver {
    private final Map<String, NamedType> map = new ConcurrentHashMap<>();

    @NonNull
    @Override
    public NamedType resolve(@NonNull String var) {
        NamedType t = map.get(var);
        if (t == null) {
            throw new NoSuchElementException("No type registered with variable '" + var + "'");
        } else {
            return t;
        }
    }

    public void save(NamedType value) {
        NamedType prev = map.put(value.getName(), value);
        if (prev != null) {
            throw new IllegalArgumentException("Duplicate type reference '" + value.getName() + "'");
        }
    }

    @Override
    public final Collection<NamedType> getDeclaredFactTypes() {
        return map.values();
    }
}
