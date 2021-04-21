package org.evrete.api;

import java.util.Set;

public interface FluentImports<T> {

    T addImport(RuleScope scope, String imp);

    default T addImport(RuleScope scope, Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Can not import " + type + ", it's canonical name is null.");
        } else {
            return addImport(scope, canonicalName);
        }
    }

    Imports getImports();

    default Set<String> getJavaImports(RuleScope... scopes) {
        return getImports().get(scopes);
    }
}
