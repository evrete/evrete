package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * <p>
 *     This class represents a compiled version of {@link RuleLiteralSources}
 * </p>
 */
public interface RuleCompiledSources<S extends RuleLiteralSources<R>, R extends Rule> {
    /**
     * Returns the sources for this compilation.
     *
     * @return the literal sources
     */
    @NonNull
    S getSources();

    /**
     * @return compiled conditions
     */
    @NonNull
    Collection<LiteralEvaluator> conditions();

    /**
     * This method returns the sources' compiled RHS.
     *
     * @return Consumer function of RhsContext if available (non-null) in the sources, or null if it's not present.
     */
    @Nullable
    Consumer<RhsContext> rhs();
}
