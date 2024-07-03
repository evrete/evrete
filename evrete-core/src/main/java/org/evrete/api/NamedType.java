package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.Objects;

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
    String getVarName();

    default boolean sameAs(NamedType other) {
        String varName1 = this.getVarName();
        String varName2 = other.getVarName();
        String typeName1 = this.getType().getName();
        String typeName2 = other.getType().getName();
        return Objects.equals(varName1, varName2) && Objects.equals(typeName1, typeName2);
    }

    /**
     * The Resolver interface is used to resolve named types based on their declared name.
     */
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
