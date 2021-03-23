package org.evrete.api;

public final class FactBuilder {
    private final String name;
    private final String unresolvedType;
    private final Class<?> resolvedType;

    private FactBuilder(String name, String unresolvedType, Class<?> resolvedType) {
        this.name = name;
        this.unresolvedType = unresolvedType;
        this.resolvedType = resolvedType;
    }

    public static FactBuilder fact(String name, String type) {
        return new FactBuilder(name, type, null);
    }

    public static FactBuilder fact(String name, Class<?> type) {
        return new FactBuilder(name, null, type);
    }

    public String getName() {
        return name;
    }

    public String getUnresolvedType() {
        return unresolvedType;
    }

    public Class<?> getResolvedType() {
        return resolvedType;
    }
}
