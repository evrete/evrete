package org.evrete.api;

import java.util.Collection;
import java.util.Objects;

public interface Named {
    static <Z extends Named> Z find(Collection<Z> collection, String name) {
        if (collection == null || collection.isEmpty()) return null;
        for (Z o : collection) {
            if (Objects.equals(o.getName(), name)) return o;
        }
        return null;
    }

    String getName();
}
