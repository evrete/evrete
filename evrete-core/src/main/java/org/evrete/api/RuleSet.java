package org.evrete.api;

import java.util.List;

public interface RuleSet<R extends Rule> {
    List<R> getRules();

}
