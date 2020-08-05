package org.evrete;

import org.evrete.api.Named;
import org.evrete.api.RhsContext;

import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRule implements Named {
    private static final Logger LOGGER = Logger.getLogger(AbstractRule.class.getName());
    private final String name;
    private final Consumer<RhsContext> NULL_RHS = arg -> LOGGER.warning("No RHS is set for rule '" + AbstractRule.this.name + '\'');
    protected Consumer<RhsContext> rhs = NULL_RHS;

    protected AbstractRule(String name) {
        this.name = name;
    }

    protected AbstractRule(AbstractRule other) {
        this.name = other.name;
        this.rhs = other.rhs;
    }

    public Consumer<RhsContext> getRhs() {
        return rhs;
    }

    @Override
    public final String getName() {
        return name;
    }

    protected final void initRhs(Consumer<RhsContext> rhs) {
        this.rhs = rhs == null ? NULL_RHS : rhs;
    }

}
