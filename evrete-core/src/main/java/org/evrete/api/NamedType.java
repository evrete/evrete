package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;

/**
 * <p>
 * A runtime representation of a fact declaration. Every {@link FactBuilder} is eventually
 * turned into an instance of this interface.
 * </p>
 */
public interface NamedType {
    /**
     * <p>
     * Method returns engine's internal {@link Type} of a fact declaration
     * </p>
     *
     * @return runtime type of fact declaration
     */
    @NonNull
    Type<?> getType();

    /**
     * <p>
     * Returns name of a fact declaration, for example "$customer"
     * </p>
     *
     * @return name of a fact declaration
     */
    String getName();

    default boolean sameAs(NamedType other) {
        return getName().equals(other.getName()) && getType().getName().equals(other.getType().getName());
    }

    interface Resolver {
        /**
         * Returns {@link NamedType} by its declared variable name
         *
         * @param var variable name
         * @return named type
         * @throws java.util.NoSuchElementException if no type is declared under the given var name
         */
        @NonNull
        NamedType resolve(@NonNull String var);

        /**
         * @return collection of currently defined named types
         */
        Collection<NamedType> getDeclaredFactTypes();
    }
}
