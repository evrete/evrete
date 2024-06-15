package org.evrete.runtime;

/**
 * A representation of {@link org.evrete.api.NamedType} with resolved Type component.
 */
class ResolvedNamedType {
    final ActiveType type;
    final String name;

    public ResolvedNamedType(ActiveType type, String name) {
        this.type = type;
        this.name = name;
    }

}
