package org.evrete.api;

/**
 * The {@code RuntimeRule} is a representation of a rule that is already associated with a {@link RuleSession}.
 */
public interface RuntimeRule extends Rule {

    RuleSession<?> getRuntime();
}
