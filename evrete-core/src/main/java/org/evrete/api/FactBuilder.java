package org.evrete.api;

/**
 * <p>
 * A fact declaration for {@link org.evrete.api.RuleBuilder}. Fact type can be specified
 * both as a Java {@link Class} or as a type name.
 * </p>
 */
public final class FactBuilder {
    private final String name;
    private final String unresolvedType;
    private final Class<?> resolvedType;

    private FactBuilder(String name, String unresolvedType, Class<?> resolvedType) {
        this.name = name;
        this.unresolvedType = unresolvedType;
        this.resolvedType = resolvedType;
    }

    /**
     * <p>
     * 'import static' this method for brevity of fact declarations.
     * </p>
     *
     * @param name fact type's reference variable
     * @param type fact's type name
     * @return returns new FactBuilder
     * @throws NullPointerException if any of the parameters is null
     */
    public static FactBuilder fact(String name, String type) {
        if (name == null || type == null) {
            throw new NullPointerException();
        } else {
            return new FactBuilder(name, type, null);
        }
    }

    /**
     * <p>
     * 'import static' this method for brevity of fact declarations.
     * </p>
     *
     * @param name fact type's reference variable
     * @param type fact's Java class
     * @return returns new FactBuilder
     * @throws NullPointerException if any of the parameters is null
     */
    public static FactBuilder fact(String name, Class<?> type) {
        if (name == null || type == null) {
            throw new NullPointerException();
        } else {
            return new FactBuilder(name, null, type);
        }
    }

    /**
     * <p>
     * The fact's variable name
     * </p>
     *
     * @return fact's variable name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Returns fact's type as String
     * </p>
     *
     * @return fact's String type or <code>null</code> if the fact is declared as Java class.
     */
    public String getUnresolvedType() {
        return unresolvedType;
    }

    /**
     * <p>
     * Returns fact's type as Java Class
     * </p>
     *
     * @return fact's declared Class or <code>null</code> if the fact's type is declared as named type.
     */
    public Class<?> getResolvedType() {
        return resolvedType;
    }
}
