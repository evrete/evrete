package org.evrete.api;

public interface RuleSetContext<C extends RuntimeContext<C>, R extends Rule> extends RuntimeContext<C>, RuleSet<R> {
}
