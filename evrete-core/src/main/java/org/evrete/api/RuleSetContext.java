package org.evrete.api;

/**
 * RuleSetContext is an interface that combines the functionality of {@link RuntimeContext} and
 * {@link RuleSet}. It is intended to be implemented by classes representing both knowledge and
 * rule sessions.
 *
 * @param <C> the type parameter representing the context.
 * @param <R> the type parameter representing the rule.
 */
public interface RuleSetContext<C extends RuntimeContext<C>, R extends Rule> extends RuntimeContext<C>, RuleSet<R> {


}
