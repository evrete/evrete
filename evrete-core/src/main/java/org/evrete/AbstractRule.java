package org.evrete;

import org.evrete.api.Imports;
import org.evrete.api.RhsContext;
import org.evrete.api.Rule;
import org.evrete.api.RuleScope;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRule implements Rule {
    private static final Logger LOGGER = Logger.getLogger(AbstractRule.class.getName());
    private String name;
    private final Consumer<RhsContext> nullRhs;
    private final Imports imports;
    private final Map<String, Object> properties;
    protected Consumer<RhsContext> rhs;
    private int salience;
    private String literalRhs;

    protected AbstractRule(String name, int defaultSalience) {
        this.name = name;
        this.properties = new ConcurrentHashMap<>();
        this.salience = defaultSalience;
        this.nullRhs = arg -> LOGGER.warning("No RHS is set for rule '" + AbstractRule.this.name + '\'');
        this.rhs = nullRhs;
        this.imports = new Imports();
    }

    protected AbstractRule(AbstractRule other, String ruleName, int salience) {
        this.name = ruleName;
        this.properties = new ConcurrentHashMap<>();
        this.properties.putAll(other.properties);
        this.salience = salience;
        this.nullRhs = other.nullRhs;
        this.rhs = other.rhs;
        this.literalRhs = other.literalRhs;
        this.imports = other.imports.copyOf();
    }

    protected void appendImports(Imports parent) {
        imports.append(parent);
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Imports getImports() {
        return imports;
    }

    @Override
    public Rule addImport(RuleScope scope, String imp) {
        this.imports.add(scope, imp);
        return this;
    }

    @Override
    public Consumer<RhsContext> getRhs() {
        return rhs;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    protected String getLiteralRhs() {
        return literalRhs;
    }

    @Override
    public final int getSalience() {
        return salience;
    }

    @Override
    public void setSalience(int salience) {
        this.salience = salience;
    }

    @Override
    public Rule set(String property, Object value) {
        this.properties.put(property, value);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T get(String property) {
        return (T) properties.get(property);
    }

    @Override
    public void setRhs(Consumer<RhsContext> rhs) {
        this.rhs = rhs == null ? nullRhs : rhs;
        this.literalRhs = null;
    }

    public void setRhs(String literalRhs) {
        this.literalRhs = literalRhs;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public void chainRhs(Consumer<RhsContext> consumer) {
        if (rhs == nullRhs || rhs == null) {
            setRhs(consumer);
        } else {
            setRhs(rhs.andThen(consumer));
        }
    }
}
