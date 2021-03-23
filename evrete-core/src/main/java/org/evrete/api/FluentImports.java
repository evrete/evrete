package org.evrete.api;

import java.util.Set;

public interface FluentImports<T> {

    T addImport(RuleScope scope, String imp);

    default T addImport(RuleScope scope, Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalStateException("Can not import " + type);
        } else {
            return addImport(scope, canonicalName);
        }
    }

    Set<String> getImports(RuleScope... scopes);
}
