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
    private final Consumer<RhsContext> NULL_RHS = arg -> LOGGER.warning("No RHS is set for rule '" + AbstractRule.this.name + '\'');
    protected Consumer<RhsContext> rhs = NULL_RHS;

    private final Map<String, Object> properties;


    protected AbstractRule(String name) {
        this.name = name;
        this.properties = new ConcurrentHashMap<>();
    }

    protected AbstractRule(AbstractRule other) {
        this.name = other.name;
        this.rhs = other.rhs;
        this.properties = new ConcurrentHashMap<>();
        this.properties.putAll(other.properties);
    }

    @Override
    public final <T> void setProperty(String name, T value) {
        this.properties.put(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(String name) {
        return (T) properties.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(String name, T defaultValue) {
        return (T) properties.getOrDefault(name, defaultValue);
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
        this.rhs = rhs == null ? NULL_RHS : rhs;
        return this;
    }


}
