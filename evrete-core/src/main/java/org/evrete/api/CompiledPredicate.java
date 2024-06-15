package org.evrete.api;

/**
 * Represents a compiled {@link ValuesPredicate} along with its source code and resolved fields.
 *
 */
public interface CompiledPredicate<C extends LiteralPredicate> {
    C getSource();

    /**
     * Compiled predicate
     * @return compiled predicate
     */
    ValuesPredicate getPredicate();

    /**
     * Returns the resolved fields in the order required by the predicate
     * @return the resolved fields
     */
    LhsField.Array<String, TypeField> resolvedFields();
}
