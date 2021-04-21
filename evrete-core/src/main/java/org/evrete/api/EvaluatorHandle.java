package org.evrete.api;

import java.util.HashSet;
import java.util.Set;

public interface EvaluatorHandle {
    double DEFAULT_COMPLEXITY = 1.0;

    FieldReference[] descriptor();

    default Set<NamedType> namedTypes() {
        Set<NamedType> set = new HashSet<>();
        for (FieldReference r : descriptor()) {
            set.add(r.type());
        }
        return set;
    }

    double getComplexity();
}
