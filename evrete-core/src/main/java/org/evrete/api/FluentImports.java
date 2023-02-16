package org.evrete.api;

import java.util.Set;

public interface FluentImports<T> {

    T addImport(String imp);

    default T addImport(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Can not import " + type + ", it's canonical name is null.");
        } else {
            return addImport(canonicalName);
        }
    }

    Imports getImports();

    default Set<String> getJavaImports() {
        return getImports().get();
    }
}
