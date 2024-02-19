package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.Collection;

/**
 * <p>
 * A wrapper for the literal components of a rule. These literal components can be:
 * </p>
 * <ul>
 *     <li>Literal (String) conditions</li>
 *     <li>(Optionally) The rule's RHS (action)</li>
 * </ul>
 * <p>
 *     During the build phase, these components are passed to a service that compiles
 *     each of them into Java method handles.
 * </p>
 */
public interface RuleLiteralSources<R extends Rule> {
    /**
     * Returns the rule associated with this instance of `LiteralRuleSources`.
     *
     * @return the rule associated with this instance of `LiteralRuleSources`
     */
    @NonNull
    R getRule();

    /**
     * Returns literal conditions associated with this instance of `LiteralRuleSources`.
     *
     * @return the conditions associated with this instance of `LiteralRuleSources`
     */
    @NonNull
    Collection<LiteralExpression> conditions();

    /**
     * Returns the right-hand side (RHS) expression associated with this instance of LiteralRuleSources. The RHS expression
     * represents the action or consequence of the rule.
     *
     * @return the RHS expression associated with this instance of LiteralRuleSources. Returns null if no RHS expression exists
     *         or if the RHS expression is represented by a functional interface.
     */
    @Nullable
    LiteralExpression rhs();
}
