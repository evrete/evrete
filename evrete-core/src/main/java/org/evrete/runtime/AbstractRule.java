package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.Rule;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.AbstractEnvironment;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Abstract base class that implements the {@link Rule} interface.
 * It provides basic functionality common to all implementations of the {@link Rule} interface.
 */
public abstract class AbstractRule extends AbstractEnvironment implements Rule {
    public static final int NULL_SALIENCE = Integer.MIN_VALUE;

    private static final Logger LOGGER = Logger.getLogger(AbstractRule.class.getName());
    private final Consumer<RhsContext> nullRhs;
    protected Consumer<RhsContext> rhs;
    private String name;
    private int salience;
    private String literalRhs;

    protected AbstractRule(AbstractEnvironment environment, String name) {
        super(environment);
        this.name = Objects.requireNonNull(name);
        this.salience = NULL_SALIENCE;
        this.nullRhs = arg -> LOGGER.warning(()->"No RHS is set for rule '" + AbstractRule.this.name + '\'');
        this.rhs = nullRhs;
    }

    protected AbstractRule(AbstractRule other, String ruleName, int salience) {
        super(other);
        this.name = ruleName;
        this.salience = salience;
        this.nullRhs = other.nullRhs;
        this.rhs = other.rhs;
        this.literalRhs = other.literalRhs;
    }

    @Override
    public Consumer<RhsContext> getRhs() {
        return rhs;
    }

    @Override
    public void setRhs(@Nullable Consumer<RhsContext> rhs) {
        this.rhs = rhs == null ? nullRhs : rhs;
        this.literalRhs = null;
    }

    public void setRhs(String literalRhs) {
        this.literalRhs = literalRhs;
    }

    @Nullable
    public String getLiteralRhs() {
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
        super.set(property, value);
        return this;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
