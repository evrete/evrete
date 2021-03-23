package org.evrete.api;

/**
 * <p>
 * A category for abstract instances that can be applied to rule's left-hand side
 * (LHS, selection and condition), right-hand side (RHS, or action) or both.
 * A class import, for example, can apply to either of these enum types.
 * </p>
 */
public enum RuleScope {
    LHS,
    RHS,
    BOTH
}
