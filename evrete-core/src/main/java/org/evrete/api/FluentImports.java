package org.evrete.api;

import java.util.Set;

public interface FluentImports<T> {

    T addImport(String imp);

    default T addImport(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalStateException("Can not import " + type);
        } else {
            return addImport(canonicalName);
        }
    }

    Set<String> getImports();
}
