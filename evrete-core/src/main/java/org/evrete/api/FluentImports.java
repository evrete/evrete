package org.evrete.api;

import java.util.Set;

/**
 * Represents an entity capable of managing Java imports in a fluent manner.
 * Allows for adding import statements using both string representations and {@link Class} objects.
 * Additionally, it provides methods to retrieve the currently added import statements.
 *
 * @param <T> the type of the implementing class to allow method chaining
 */
public interface FluentImports<T> {

    /**
     * Adds an import statement using its string representation.
     *
     * @param imp the full canonical name of the class or package to be imported
     * @return this instance to allow for method chaining
     */
    T addImport(String imp);

    default T addImport(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Can not import " + type + ", it's canonical name is null.");
        } else {
            return addImport(canonicalName);
        }
    }

    /**
     * Retrieves the current set of import statements.
     *
     * @return an {@link Imports} object containing the current set of import statements
     */
    Imports getImports();

    /**
     * Retrieves a set containing the string representations of Java import statements.
     *
     * @return a {@link Set} of strings representing the current Java import statements
     */
    default Set<String> getJavaImports() {
        return getImports().get();
    }
}
