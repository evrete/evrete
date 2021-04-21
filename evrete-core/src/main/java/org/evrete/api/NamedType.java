package org.evrete.api;

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
     * @return runtime type of a fact declaration
     */
    Type<?> getType();

    /**
     * <p>
     * Returns name of a fact declaration, for example "$customer"
     * </p>
     *
     * @return name of a fact declaration
     */
    String getName();

    interface Resolver {
        NamedType resolve(String var);
    }
}
