package org.evrete.api;

import java.util.HashSet;
import java.util.Set;

public interface EvaluatorHandle extends WorkUnit {

    FieldReference[] descriptor();

    default Set<NamedType> namedTypes() {
        Set<NamedType> set = new HashSet<>();
        for (FieldReference r : descriptor()) {
            set.add(r.type());
        }
        return set;
    }
}
