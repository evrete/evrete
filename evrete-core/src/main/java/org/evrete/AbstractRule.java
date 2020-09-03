package org.evrete;

import org.evrete.api.RhsContext;
import org.evrete.api.Rule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRule implements Rule {
    private static final Logger LOGGER = Logger.getLogger(AbstractRule.class.getName());
    private final String name;
    private final Consumer<RhsContext> nullRhs;
    protected Consumer<RhsContext> rhs;
    private int salience;

    private final Map<String, Object> properties;

    protected AbstractRule(String name, int defaultSalience) {
        this.name = name;
        this.properties = new ConcurrentHashMap<>();
        this.salience = defaultSalience;
        this.nullRhs = arg -> LOGGER.warning("No RHS is set for rule '" + AbstractRule.this.name + '\'');
        this.rhs = nullRhs;
    }

    protected AbstractRule(AbstractRule other) {
        this.name = other.name;
        this.properties = new ConcurrentHashMap<>();
        this.properties.putAll(other.properties);
        this.salience = other.salience;
        this.nullRhs = other.nullRhs;
        this.rhs = other.rhs;
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
    public final <T> void setProperty(String property, T value) {
        this.properties.put(property, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(String property) {
        return (T) properties.get(property);
    }

    @Override
    public final Consumer<RhsContext> getRhs() {
        return rhs;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public Rule setRhs(Consumer<RhsContext> rhs) {
        this.rhs = rhs == null ? nullRhs : rhs;
        return this;
    }

    @Override
    public Rule chainRhs(Consumer<RhsContext> consumer) {
        if(rhs == nullRhs || rhs == null) {
            setRhs(consumer);
        } else {
            setRhs(rhs.andThen(consumer));
        }
        return this;
    }


}
