package org.evrete;

import org.evrete.api.RhsContext;
import org.evrete.api.Rule;

import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRule implements Rule {
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

    @Override
    public Consumer<RhsContext> getRhs() {
        return rhs;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void setRhs(Consumer<RhsContext> rhs) {
        this.rhs = rhs == null ? NULL_RHS : rhs;
    }


}
