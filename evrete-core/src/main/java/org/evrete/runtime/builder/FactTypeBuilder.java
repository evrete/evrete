package org.evrete.runtime.builder;

import org.evrete.api.FieldReference;
import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class FactTypeBuilder implements NamedType {
    private final AbstractLhsBuilder<?, ?> group;
    private final String var;
    private final Type<?> type;
    //TODO avoid keeping this set inside the builder
    private final Set<TypeField> betaFields = new HashSet<>();

    FactTypeBuilder(AbstractLhsBuilder<?, ?> group, String var, Type<?> type) {
        Objects.requireNonNull(var);
        Objects.requireNonNull(type);
        this.group = group;
        this.var = var;
        this.type = type;
    }

    void addBetaField(FieldReference ref) {
        this.betaFields.add(ref.field());
    }

    public final Set<TypeField> getBetaTypeFields() {
        return this.betaFields;
    }

    @Override
    public String getVar() {
        return var;
    }

    public AbstractLhsBuilder<?, ?> getGroup() {
        return group;
    }

    @Override
    public Type<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "{" +
                "var='" + var + '\'' +
                ", type=" + type +
                '}';
    }
}
